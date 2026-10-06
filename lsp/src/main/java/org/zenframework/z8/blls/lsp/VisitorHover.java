package org.zenframework.z8.blls.lsp;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.analyzer.MapInt;
import org.zenframework.z8.blls.ast.*;

import java.util.Iterator;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

abstract class VisitorHover extends VisitorHit {
  protected abstract void success(Hover value);

  MarkupContent markupOfEnumeration(Node<org.zenframework.z8.blls.ast.Enumeration> node) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "`enum` %s%s".formatted(
        node.data().name(ast()),
        ast().docOf(node).map(it -> "\n\n" + it).orElse("")
      )
    );
  }

  private MarkupContent markupOfDeclaration(Node<org.zenframework.z8.blls.ast.Declaration> node) {
    MapInt<?>.Keys dejavuAnnotations = new MapInt<>().keySet();

    Iterator<String> lines = ast().analyzer().inheritanceKlasses(node).map(klass -> {
      String result = "[%s](%s)".formatted(klass.data().name(ast()), ast().uriOf(klass));

      String annotations = klass.data()
        .annotations(ast())
        .map(annotation -> {
          boolean shadowed = !dejavuAnnotations.add(annotation.data().nameId());

          return "%s[[%s](%s)%s]%s".formatted(
            shadowed ? "~~" : "",
            annotation.data().name(ast()),
            ast().uriOf(annotation),
            ast().analyzer()
              .annotationConstant(annotation)
              .filter(not(it -> it instanceof Expression.Type.Null))
              .map(it -> it instanceof String v && v.length() > 10 ? v.substring(0, 10) + "…" : it)
              .map(it -> it instanceof String v ? '"' + v + '"' : it)
              .map(it -> " " + it)
              .orElse(""),
            shadowed ? "~~" : ""
          );
        })
        .collect(joining(" "));

      if (!annotations.isEmpty())
        result = result + " " + annotations;

      return result;
    }).iterator();

    return switch (ast().parent(node.id())) {
      case Node(int id, Klass v) -> {
        var sb = new StringBuilder(lines.next());

        if (lines.hasNext()) sb.append('\n');
        lines.forEachRemaining(it -> sb.append('\n').append("- extends ").append(it));

        ast().docOf(new Node<>(id, v)).ifPresent(it -> sb.append("\n\n").append(it));

        yield new MarkupContent(
          MarkupKind.MARKDOWN,
          sb.toString()
        );
      }
      case Node(int _, Expression.Object _) -> {
        var sb = new StringBuilder("object");

        if (lines.hasNext()) sb.append('\n');
        lines.forEachRemaining(it -> sb.append('\n').append("- extends ").append(it));

        ast().analyzer()
          .inheritanceKlasses(node)
          .findFirst()
          .flatMap(ast()::docOf)
          .ifPresent(it -> sb.append("\n\n").append(it));

        yield new MarkupContent(
          MarkupKind.MARKDOWN,
          sb.toString()
        );
      }
      default -> throw new AssertionError();
    };
  }

  @Override
  protected void hitKlassName(Node<Klass> node) {
    success(new Hover(
      markupOfDeclaration(node.data().child(ast())),
      ast().range(ast().klassName(node))
    ));
  }

  @Override
  protected void hitEnumerationName(Node<Enumeration> node) {
    success(new Hover(
      markupOfEnumeration(node),
      ast().range(ast().enumerationName(node))
    ));
  }

  @Override
  protected void hitDeclarationFieldName(Node<Declaration.Field> node) {
    success(new Hover(
      ast().markupOfDeclarationField(node),
      ast().range(ast().declarationFieldName(node))
    ));
  }

  @Override
  protected void hitDeclarationFieldOperation(Node<Declaration.Field> node) {
    success(new Hover(
      ast().markupOfDeclarationField(node),
      ast().range(ast().declarationFieldOperation(node).orElseThrow())
    ));
  }

  @Override
  protected void hitDeclarationMethodName(Node<Declaration.Method> node) {
    success(new Hover(
      ast().markupOfDeclarationMethod(node),
      ast().range(ast().declarationMethodName(node))
    ));
  }

  @Override
  protected void hitDeclarationPriorityKeyword(Node<Declaration.Priority> node) {
    success(new Hover(
      ast().markupOfDeclarationPriority(node),
      ast().range(ast().declarationPriorityKeyword(node))
    ));
  }

  @Override
  protected void hitDeclarationCastKeyword(Node<Declaration.Cast> node) {
    success(new Hover(
      ast().markupOfDeclarationCast(node),
      ast().range(ast().declarationCastKeyword(node))
    ));
  }

  @Override
  protected void hitDeclarationCastDefinitionReference(Node<Declaration.Cast> node) {
    success(new Hover(
      ast().markupOfDeclarationCast(node),
      ast().range(ast().declarationCastKeyword(node))
    ));
  }

  @Override
  protected void hitDeclarationOperatorKeyword(Node<Declaration.Operator> node) {
    success(new Hover(
      ast().markupOfDeclarationOperator(node),
      ast().range(ast().declarationOperatorKeyword(node))
    ));
  }

  @Override
  protected void hitDeclarationOperatorOperation(Node<Declaration.Operator> node) {
    success(new Hover(
      ast().markupOfDeclarationOperator(node),
      ast().range(ast().declarationOperatorOperation(node))
    ));
  }

  @Override
  protected void hitDeclarationRecordsChildName(Node<Declaration.Records.Child> node) {
    success(new Hover(
      ast().markupOfDeclarationRecordsChild(node),
      ast().range(ast().declarationRecordsChildName(node))
    ));
  }

  @Override
  protected void hitEnumerationChildName(Node<Enumeration.Child> node) {
    success(new Hover(
      ast().markupOfEnumerationChild(node),
      ast().range(ast().enumerationChildName(node))
    ));
  }

  @Override
  protected void hitStatementVariableOperation(Node<Statement.Variable> node) {
    ast().analyzer()
      .extra(node, Extra.StatementVariable.class)
      .resolvedAssign(ast())
      .ifPresent(it -> success(new Hover(
        ast().markupOfDeclarationOperator(it),
        ast().anchor(it)
      )));
  }

  @Override
  protected void hitExpressionThis(Node<Expression.This> node) {
    if (ast().type(node).orElse(null) instanceof Expression.Type.Declaration v) {
      success(new Hover(
        markupOfDeclaration(v.node(ast())),
        ast().range(node)
      ));
    }
  }

  @Override
  protected void hitExpressionSuper(Node<Expression.Super> node) {
    if (ast().type(node).orElse(null) instanceof Expression.Type.Declaration v) {
      success(new Hover(
        markupOfDeclaration(v.node(ast())),
        ast().range(node)
      ));
    }
  }

  @Override
  protected void hitExpressionContainerKeyword(Node<Expression.Container> node) {
    if (ast().type(node).orElse(null) instanceof Expression.Type.Declaration v) {
      success(new Hover(
        markupOfDeclaration(v.node(ast())),
        ast().range(node)
      ));
    }
  }

  @Override
  protected void hitExpressionAccessName(Node<Expression.Access> node) {
    super.hitExpressionAccessName(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionAccess.class);
    switch (e.getResolved(ast()).orElse(null)) {
      case null -> {
      }
      case Extra.ExpressionAccess.Resolved.Pack _ -> {
        if (ast().parent(node.id()) instanceof Node(int id, Expression.Access v)) {
          hitExpressionAccessName(new Node<>(id, v));
        }
      }
      case Extra.ExpressionAccess.Resolved.Declaration v -> success(new Hover(
        markupOfDeclaration(v.node(ast())),
        ast().range(node)
      ));
      case Extra.ExpressionAccess.Resolved.Enumeration v -> success(new Hover(
        markupOfEnumeration(v.node(ast())),
        ast().range(node)
      ));
      case Extra.ExpressionAccess.Resolved.DeclarationField(int id) -> {
        Node<Declaration.Field> field = ast().node(id);
        MapInt<?>.Keys dejavu = new MapInt<>().keySet();
        Iterator<String> lines = ast().analyzer()
          .annotationsOf(ast().analyzer().declarationOf(node), field)
          .map(annotation -> {
            boolean shadowed = !dejavu.add(annotation.data().nameId());

            return "- %s: %s[[%s](%s)%s]%s".formatted(
              switch (ast().parent(ast().analyzer().declarationOf(annotation).id()).data()) {
                case Klass v -> v.name(ast());
                case Expression.Object _ -> "object";
                default -> throw new AssertionError();
              },
              shadowed ? "~~" : "",
              annotation.data().name(ast()),
              ast().uriOf(annotation),
              ast().analyzer()
                .annotationConstant(annotation)
                .filter(not(it -> it instanceof Expression.Type.Null))
                .map(it -> it instanceof String v && v.length() > 10 ? v.substring(0, 10) + "…" : it)
                .map(it -> it instanceof String v ? '"' + v + '"' : it)
                .map(it -> " " + it)
                .orElse(""),
              shadowed ? "~~" : ""
            );
          })
          .iterator();

        var sb = new StringBuilder(
          "%s %s::%s".formatted(
            ast().analyzer().typeOf(field.data().definition(ast())).map(ast()::toMarkdown).orElse("<error>"),
            ast().toMarkdown(new Expression.Type.Declaration(ast().analyzer().declarationOf(node).id())),
            node.data().name(ast())
          )
        );

        if (lines.hasNext()) sb.append('\n');
        lines.forEachRemaining(it -> sb.append('\n').append(it));

        ast().docOf(field).ifPresent(it -> sb.append("\n\n").append(it));

        success(new Hover(
          new MarkupContent(
            MarkupKind.MARKDOWN,
            sb.toString()
          ),
          ast().range(ast().expressionAccessName(node))
        ));
      }
      case Extra.ExpressionAccess.Resolved.DeclarationRecordsChild(int id) -> success(new Hover(
        ast().markupOfDeclarationRecordsChild(ast().node(id)),
        ast().range(ast().expressionAccessName(node))
      ));
      case Extra.ExpressionAccess.Resolved.EnumerationChild(int id) -> success(new Hover(
        ast().markupOfEnumerationChild(ast().node(id)),
        ast().range(ast().expressionAccessName(node))
      ));
      case Extra.ExpressionAccess.Resolved.Parameter(int id) -> {
        Node<Parameter> n = ast().node(id);
        success(new Hover(
          new MarkupContent(
            MarkupKind.MARKDOWN,
            "%s %s".formatted(
              ast().analyzer()
                .typeOf(n.data().definition(ast()))
                .map(ast()::toMarkdown)
                .orElse("<error>"),
              n.data().name(ast())
            )
          ),
          ast().range(ast().expressionAccessName(node))
        ));
      }
      case Extra.ExpressionAccess.Resolved.StatementVariable(int id) -> {
        Node<Statement.Variable> n = ast().node(id);
        success(new Hover(
          new MarkupContent(
            MarkupKind.MARKDOWN,
            "%s %s".formatted(
              ast().analyzer()
                .typeOf(n.data().definition(ast()))
                .map(ast()::toMarkdown)
                .orElse("<error>"),
              n.data().name(ast())
            )
          ),
          ast().range(ast().expressionAccessName(node))
        ));
      }
      case Extra.ExpressionAccess.Resolved.StatementLoopForEach(int id) -> {
        Node<Statement.LoopForEach> n = ast().node(id);
        success(new Hover(
          new MarkupContent(
            MarkupKind.MARKDOWN,
            "%s %s".formatted(
              ast().analyzer()
                .typeOf(n.data().definition(ast()))
                .map(ast()::toMarkdown)
                .orElse("<error>"),
              n.data().name(ast())
            )
          ),
          ast().range(ast().expressionAccessName(node))
        ));
      }
      case Extra.ExpressionAccess.Resolved.StatementTryCatch(int id) -> {
        Node<Statement.Try.Catch> n = ast().node(id);
        success(new Hover(
          new MarkupContent(
            MarkupKind.MARKDOWN,
            "%s %s".formatted(
              ast().analyzer()
                .typeOf(n.data().definition(ast()))
                .map(ast()::toMarkdown)
                .orElse("<error>"),
              n.data().name(ast())
            )
          ),
          ast().range(ast().expressionAccessName(node))
        ));
      }
    }
  }

  @Override
  protected void hitExpressionInvokeName(Node<Expression.Invoke> node) {
    super.hitExpressionInvokeName(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionInvoke.class);
    switch (e.getResolved(ast()).orElse(null)) {
      case null -> {
      }
      case Extra.ExpressionInvoke.Resolved.DeclarationMethod v -> success(new Hover(
        ast().markupOfDeclarationMethod(v.node(ast())),
        ast().range(ast().expressionInvokeName(node))
      ));
      case Extra.ExpressionInvoke.Resolved.DeclarationCast v -> success(new Hover(
        ast().markupOfDeclarationCast(v.node(ast())),
        ast().range(ast().expressionInvokeName(node))
      ));
    }
    if (e.resolvedSynthetic != null) {
      success(new Hover(
        ast().markupOfSyntheticMethod(e.resolvedSynthetic),
        ast().range(ast().expressionInvokeName(node))
      ));
    }
  }

  @Override
  protected void hitExpressionObjectKeyword(Node<Expression.Object> node) {
    super.hitExpressionObjectKeyword(node);

    success(new Hover(
      markupOfDeclaration(node.data().child(ast())),
      ast().range(ast().expressionObjectKeyword(node))
    ));
  }

  @Override
  protected void hitExpressionUnaryOperation(Node<Expression.Unary> node) {
    super.hitExpressionUnaryOperation(node);

    ast().analyzer()
      .extra(node, Extra.ExpressionUnary.class)
      .resolved(ast())
      .ifPresent(it -> success(new Hover(
        ast().markupOfDeclarationOperator(it),
        ast().range(ast().expressionUnaryOperation(node))
      )));
  }

  @Override
  protected void hitExpressionBinaryOperation(Node<Expression.Binary> node) {
    super.hitExpressionBinaryOperation(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
    e.resolved(ast()).ifPresent(
      it -> success(new Hover(
        ast().markupOfDeclarationOperator(it),
        ast().range(ast().expressionBinaryOperation(node))
      )));
    if (e.resolvedSynthetic != null) {
      success(new Hover(
        ast().markupOfSyntheticOperator(e.resolvedSynthetic),
        ast().range(ast().expressionBinaryOperation(node))
      ));
    }
    e.resolvedAssign(ast()).ifPresent(it -> success(new Hover(
      ast().markupOfDeclarationOperator(it),
      ast().range(ast().expressionBinaryOperation(node))
    )));
  }

  @Override
  protected void hitDefinitionReferenceName(Node<Definition.Reference> node) {
    super.hitDefinitionReferenceName(node);

    var e = ast().analyzer().extra(node, Extra.DefinitionReference.class);
    switch (e.getResolved(ast()).orElse(null)) {
      case null -> {
      }
      case Ast.Pack _ -> {
        if (ast().parent(node.id()) instanceof Node(int id, Definition.Reference v)) {
          hitDefinitionReferenceName(new Node<>(id, v));
        }
      }
      case Ast.Declaration v -> success(new Hover(
        markupOfDeclaration(v.node(ast())),
        ast().range(node)
      ));
      case Ast.Enumeration v -> success(new Hover(
        markupOfEnumeration(v.node(ast())),
        ast().range(node)
      ));
    }
  }
}
