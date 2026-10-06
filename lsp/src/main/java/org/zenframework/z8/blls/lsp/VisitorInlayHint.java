package org.zenframework.z8.blls.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

abstract class VisitorInlayHint extends Visitor {
  protected abstract void success(InlayHint value);

  protected abstract Optional<Expression.Type.Declaration> primaryOf(String name);

  protected abstract Location locationOf(Node<?> node);

  protected abstract Range range();

  protected abstract boolean inlayHintBoolean();

  protected abstract boolean inlayHintNonObvious();

  protected abstract boolean inlayHintImplicit();

  @Override
  protected abstract Ast ast();

  private void cast(Node<?> node, int castId) {
    if (inlayHintImplicit() && castId > 0) {
      Node<Declaration.Cast> cast = ast().node(castId);
      var ihlp = new InlayHintLabelPart("(%s)".formatted(
        ast().toString(ast().analyzer().typeOf(cast.data().definition(ast())).orElseThrow())
      ));
      ihlp.setTooltip(ast().markupOfDeclarationCast(cast));
      ihlp.setLocation(locationOf(cast));
      var ih = new InlayHint(
        ast().position(ast().start(node)),
        Either.forRight(List.of(ihlp))
      );
      if (node.data() instanceof Expression.Binary)
        ih.setPaddingRight(true);
      success(ih);
    }
  }

  @Override
  public void visit(int id) {
    var r = ast().range(ast().node(id));
    if (Ast.intersects(range(), r))
      super.visit(id);
  }

  @Override
  protected void declarationField(Node<Declaration.Field> node) {
    super.declarationField(node);

    var e = ast().analyzer().extra(node, Extra.DeclarationField.class);
    node.data().value(ast()).ifPresent(it -> cast(it.data().value(ast()), e.valueCastId));
  }

  @Override
  protected void statementVariable(Node<Statement.Variable> node) {
    super.statementVariable(node);

    var e = ast().analyzer().extra(node, Extra.StatementVariable.class);
    node.data().value(ast()).ifPresent(it -> cast(it, e.valueCastId));
  }

  @Override
  protected void statementReturn(Node<Statement.Return> node) {
    super.statementReturn(node);

    var e = ast().analyzer().extra(node, Extra.StatementReturn.class);
    node.data().value(ast()).ifPresent(it -> cast(it, e.valueCastId));
    e.resolvedPriority(ast())
      .filter(_ -> inlayHintImplicit())
      .ifPresent(it -> {
        var v = node.data().value(ast()).orElseThrow();
        var ihlp = new InlayHintLabelPart(".operatorPriority()");
        ihlp.setTooltip(ast().markupOfDeclarationPriority(it));
        ihlp.setLocation(locationOf(it));
        var ih = new InlayHint(
          ast().position(ast().end(v)),
          Either.forRight(List.of(ihlp))
        );
        if (v.data() instanceof Expression.Binary)
          ih.setPaddingLeft(true);
        success(ih);
      });
  }

  @Override
  protected void statementThrow(Node<Statement.Throw> node) {
    super.statementThrow(node);

    var e = ast().analyzer().extra(node, Extra.StatementThrow.class);
    cast(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void statementCondition(Node<Statement.Condition> node) {
    super.statementCondition(node);

    var e = ast().analyzer().extra(node, Extra.StatementCondition.class);
    cast(node.data().condition(ast()), e.conditionCastId);
  }

  @Override
  protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
    super.statementLoopWhile(node);

    var e = ast().analyzer().extra(node, Extra.StatementLoopWhile.class);
    cast(node.data().condition(ast()), e.conditionCastId);
  }

  @Override
  protected void statementLoopFor(Node<Statement.LoopFor> node) {
    super.statementLoopFor(node);

    var e = ast().analyzer().extra(node, Extra.StatementLoopFor.class);
    cast(node.data().condition(ast()), e.conditionCastId);
  }

  @Override
  protected void expressionParen(Node<Expression.Paren> node) {
    super.expressionParen(node);

    ast().analyzer()
      .extra(node, Extra.ExpressionParen.class)
      .resolvedPriority(ast())
      .filter(_ -> inlayHintImplicit())
      .ifPresent(it -> {
        var ihlp = new InlayHintLabelPart(".operatorPriority()");
        ihlp.setTooltip(ast().markupOfDeclarationPriority(it));
        ihlp.setLocation(locationOf(it));
        var ih = new InlayHint(
          ast().position(ast().end(node.data().child(ast()))),
          Either.forRight(List.of(ihlp))
        );
        if (node.data().child(ast()).data() instanceof Expression.Binary)
          ih.setPaddingLeft(true);
        success(ih);
      });
  }

  @Override
  protected void expressionIndex(Node<Expression.Index> node) {
    super.expressionIndex(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionIndex.class);
    cast(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void expressionInvoke(Node<Expression.Invoke> node) {
    super.expressionInvoke(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionInvoke.class);
    if (e.getResolved(ast()).orElse(null) instanceof Extra.ExpressionInvoke.Resolved.DeclarationMethod v) {
      var arguments = node.data()
        .arguments(ast())
        .toList();
      var parameters = v.node(ast())
        .data()
        .parameters(ast())
        .toList();

      assert arguments.size() == parameters.size();

      IntStream.range(0, arguments.size())
        .filter(i -> {
          // inlayHintNonObviuos(): работать только с "неочевидными" аргументами
          // Очевидный аргумент оканчивается именем, совпадающим с названием параметра
          if (inlayHintNonObvious()) {
            return switch (arguments.get(i).data().value(ast()).data()) {
              case Expression.Access v2 -> v2.nameId() != parameters.get(i).data().nameId();
              case Expression.Invoke v2 -> v2.nameId() != parameters.get(i).data().nameId();
              default -> true;
            };
          }

          // inlayHintBoolean: работать только с параметрами типа bool/sql_bool
          return inlayHintBoolean() && ast().analyzer()
            .typeOf(parameters.get(i).data().definition(ast()))
            .filter(it ->
              it.equals(primaryOf("bool").orElse(null)) ||
                it.equals(primaryOf("sql_bool").orElse(null))
            )
            .isPresent();
        })
        .forEach(i -> {
          var ihlp = new InlayHintLabelPart("%s:".formatted(parameters.get(i).data().name(ast())));
          ihlp.setLocation(locationOf(parameters.get(i)));
          var ih = new InlayHint(
            ast().position(ast().start(arguments.get(i))),
            Either.forRight(List.of(ihlp))
          );
          ih.setKind(InlayHintKind.Parameter);
          ih.setPaddingRight(true);
          success(ih);
        });
    }
  }

  @Override
  protected void expressionUnary(Node<Expression.Unary> node) {
    super.expressionUnary(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionUnary.class);
    cast(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void expressionBinary(Node<Expression.Binary> node) {
    super.expressionBinary(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
    cast(node.data().left(ast()), e.leftCastId);
    cast(node.data().right(ast()), e.rightCastId);
    cast(node, e.rightCastAssignId);
  }

  @Override
  protected void expressionTernary(Node<Expression.Ternary> node) {
    super.expressionTernary(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionTernary.class);
    cast(node.data().condition(ast()), e.conditionCastId);
    cast(node.data().then(ast()), e.thenCastId);
    cast(node.data().other(ast()), e.otherCastId);
  }

  @Override
  protected void expressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
    super.expressionArrayChildValue(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionArrayChildValue.class);
    cast(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void expressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
    super.expressionArrayChildEntry(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionArrayChildEntry.class);
    cast(node.data().key(ast()), e.keyCastId);
    cast(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void expressionInvokeArgument(Node<Expression.Invoke.Argument> node) {
    super.expressionInvokeArgument(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionInvokeArgument.class);
    cast(node.data().value(ast()), e.valueCastId);
  }
}
