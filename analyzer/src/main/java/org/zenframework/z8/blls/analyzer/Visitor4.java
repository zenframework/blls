package org.zenframework.z8.blls.analyzer;

import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

import java.util.stream.Stream;

/// Шаг 4:
/// - Предупредить о неиспользуемых включениях
/// - Предупредить о доступе к устаревшим объявлениям
/// - Предупредить о доступе к статическим элементам из объектного контекста
/// - Предупредить о неявных операторах приведения
public abstract class Visitor4 extends Visitor {
  @Override
  protected abstract Ast ast();

  /// Не используется
  protected abstract void diagnosticImportUnused(Node<Import> node);

  /// Устарело
  protected abstract void diagnosticDeprecated(Node<?> node, Node<?> target, Node<Annotation> annotationDeprecated);

  /// Доступ к статическому элементу из объектного контекста
  protected abstract void diagnosticStatic(Node<?> node, Node<?> target);

  /// Неявное использование оператора преобразования
  protected abstract void diagnosticImplicitCast(Node<?> node, Node<Declaration.Cast> cast);

  /// Повторное объявление
  protected abstract void diagnosticDuplicate(Node<Declaration.Initialize> nodeOld, Node<Declaration.Initialize> nodeNew);

  /// Аннотация применена в родительском классе
  protected abstract void diagnosticAnnotation(Node<Annotation> node, Node<Annotation> shadows);


  @Override
  protected void import_(Node<Import> node) {
    if (!ast().extra(node, Extra.Import.class).used)
      diagnosticImportUnused(node);
  }

  @Override
  protected void declaration(Node<Declaration> node) {
    super.declaration(node);

    node.data()
      .children(ast())
      .gather(Ast.only(Declaration.Initialize.class))
      // just compare by accessed names, skip complex type logic
      .gather(Ast.duplicates(initialize ->
        ast().initializeAccesses(initialize.data())
          .map(it -> it.data().nameId())
          .toList()
      ))
      .forEach(it -> diagnosticDuplicate(ast().node(it.oldId()), ast().node(it.newId())));
  }

  @Override
  protected void declarationField(Node<Declaration.Field> node) {
    super.declarationField(node);

    var v = node.data().value(ast()).orElse(null);
    if (v == null) return;

    var e = ast().extra(node, Extra.DeclarationField.class);
    checkDeprecatedMaybe(node, e.resolvedAssignId);
    checkImplicitCastMaybe(v, e.valueCastId);
  }

  @Override
  protected void declarationInitialize(Node<Declaration.Initialize> node) {
    super.declarationInitialize(node);

    ast().fieldOf(node.data()).ifPresent(field -> {
      var previouslyDefinedAnnotations = ast()
        .inheritance(ast().declarationOf(node))
        .skip(1)
        .findFirst()
        .stream()
        .flatMap(it -> ast().annotationsOf(it, field))
        .toList();

      node.data()
        .annotations(ast())
        .forEach(nodeNew ->
          ast().annotationConstant(nodeNew).flatMap(v ->
            previouslyDefinedAnnotations.stream()
              .filter(nodeOld -> v.equals(ast().annotationConstant(nodeOld).orElse(null)))
              .findFirst()
          ).ifPresent(nodeOld -> diagnosticAnnotation(nodeNew, nodeOld))
        );
    });
  }

  @Override
  protected void declarationMethod(Node<Declaration.Method> node) {
    super.declarationMethod(node);
  }

  @Override
  protected void declarationPriority(Node<Declaration.Priority> node) {
    super.declarationPriority(node);
  }

  @Override
  protected void declarationCast(Node<Declaration.Cast> node) {
    super.declarationCast(node);
  }

  @Override
  protected void declarationOperator(Node<Declaration.Operator> node) {
    super.declarationOperator(node);
  }

  @Override
  protected void statementVariable(Node<Statement.Variable> node) {
    super.statementVariable(node);

    var v = node.data().value(ast()).orElse(null);
    if (v == null) return;

    var e = ast().extra(node, Extra.StatementVariable.class);
    checkDeprecatedMaybe(v, e.resolvedAssignId);
    checkImplicitCastMaybe(v, e.valueCastId);
  }

  @Override
  protected void statementReturn(Node<Statement.Return> node) {
    super.statementReturn(node);

    var v = node.data().value(ast()).orElse(null);
    if (v == null) return;

    var e = ast().extra(node, Extra.StatementReturn.class);
    checkDeprecatedMaybe(v, e.resolvedPriorityId);
    checkImplicitCastMaybe(v, e.valueCastId);
  }

  @Override
  protected void statementThrow(Node<Statement.Throw> node) {
    super.statementThrow(node);

    var e = ast().extra(node, Extra.StatementThrow.class);
    checkImplicitCastMaybe(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void statementCondition(Node<Statement.Condition> node) {
    super.statementCondition(node);

    var e = ast().extra(node, Extra.StatementCondition.class);
    checkImplicitCastMaybe(node.data().condition(ast()), e.conditionCastId);
  }

  @Override
  protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
    super.statementLoopWhile(node);

    var e = ast().extra(node, Extra.StatementLoopWhile.class);
    checkImplicitCastMaybe(node.data().condition(ast()), e.conditionCastId);
  }

  @Override
  protected void statementLoopFor(Node<Statement.LoopFor> node) {
    super.statementLoopFor(node);

    var e = ast().extra(node, Extra.StatementLoopFor.class);
    checkImplicitCastMaybe(node.data().condition(ast()), e.conditionCastId);
  }

  @Override
  protected void expressionParen(Node<Expression.Paren> node) {
    super.expressionParen(node);

    var e = ast().extra(node, Extra.ExpressionParen.class);
    checkDeprecatedMaybe(node.data().child(ast()), e.resolvedPriorityId);
  }

  @Override
  protected void expressionAccess(Node<Expression.Access> node) {
    super.expressionAccess(node);

    var e = ast().extra(node, Extra.ExpressionAccess.class);
    switch (e.getResolved(ast()).orElse(null)) {
      case Extra.ExpressionAccess.Resolved.Declaration(int id) -> checkDeprecatedMaybe(node, ast().parent(id).id());
      case Extra.ExpressionAccess.Resolved.Enumeration(int id) -> checkDeprecatedMaybe(node, id);
      case Extra.ExpressionAccess.Resolved.DeclarationField(int id) -> {
        checkDeprecatedMaybe(node, id);
        if (
          (Declaration.STATIC & ast().<Declaration.Field>node(id).data().modifiers()) != 0 &&
            node.data().child(ast()).flatMap(ast()::type).isPresent()
        ) {
          diagnosticStatic(node, ast().node(id));
        }
      }
      case Extra.ExpressionAccess.Resolved.DeclarationRecordsChild(int id) -> {
        checkDeprecatedMaybe(node, id);
        if (node.data().child(ast()).flatMap(ast()::type).isPresent()) {
          diagnosticStatic(node, ast().node(id));
        }
      }
      case null, default -> {
      }
    }
  }

  @Override
  protected void expressionIndex(Node<Expression.Index> node) {
    super.expressionIndex(node);

    var e = ast().extra(node, Extra.ExpressionIndex.class);
    checkImplicitCastMaybe(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void expressionInvoke(Node<Expression.Invoke> node) {
    super.expressionInvoke(node);

    var e = ast().extra(node, Extra.ExpressionInvoke.class);
    switch (e.getResolved(ast()).orElse(null)) {
      case null -> {
      }
      case Extra.ExpressionInvoke.Resolved.DeclarationMethod v -> {
        checkDeprecatedMaybe(node, v.id());
        if (
          (Declaration.STATIC & v.node(ast()).data().modifiers()) != 0 &&
            node.data().child(ast()).flatMap(ast()::type).isPresent()
        ) {
          diagnosticStatic(node, v.node(ast()));
        }
      }
      case Extra.ExpressionInvoke.Resolved.DeclarationCast(int id) -> checkDeprecatedMaybe(node, id);
    }
  }

  @Override
  protected void expressionCast(Node<Expression.Cast> node) {
    super.expressionCast(node);

    var e = ast().extra(node, Extra.ExpressionCast.class);
    checkDeprecatedMaybe(node, e.valueCastId);
  }

  @Override
  protected void expressionUnary(Node<Expression.Unary> node) {
    super.expressionUnary(node);

    var e = ast().extra(node, Extra.ExpressionUnary.class);
    checkDeprecatedMaybe(node, e.resolvedId);
    checkImplicitCastMaybe(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void expressionBinary(Node<Expression.Binary> node) {
    super.expressionBinary(node);

    var e = ast().extra(node, Extra.ExpressionBinary.class);
    checkDeprecatedMaybe(node, e.resolvedId);
    checkDeprecatedMaybe(node, e.resolvedAssignId);
    checkImplicitCastMaybe(node.data().left(ast()), e.leftCastId);
    checkImplicitCastMaybe(node.data().right(ast()), e.rightCastId);
    checkImplicitCastMaybe(node.data().right(ast()), e.rightCastAssignId);
  }

  @Override
  protected void expressionTernary(Node<Expression.Ternary> node) {
    super.expressionTernary(node);

    var e = ast().extra(node, Extra.ExpressionTernary.class);
    checkImplicitCastMaybe(node.data().condition(ast()), e.conditionCastId);
    checkImplicitCastMaybe(node.data().then(ast()), e.thenCastId);
    checkImplicitCastMaybe(node.data().other(ast()), e.otherCastId);
  }

  @Override
  protected void expressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
    super.expressionArrayChildValue(node);

    var e = ast().extra(node, Extra.ExpressionArrayChildValue.class);
    checkImplicitCastMaybe(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void expressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
    super.expressionArrayChildEntry(node);

    var e = ast().extra(node, Extra.ExpressionArrayChildEntry.class);
    checkImplicitCastMaybe(node.data().key(ast()), e.keyCastId);
    checkImplicitCastMaybe(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void expressionInvokeArgument(Node<Expression.Invoke.Argument> node) {
    super.expressionInvokeArgument(node);

    var e = ast().extra(node, Extra.ExpressionInvokeArgument.class);
    checkImplicitCastMaybe(node.data().value(ast()), e.valueCastId);
  }

  @Override
  protected void definitionReference(Node<Definition.Reference> node) {
    super.definitionReference(node);

    var e = ast().extra(node, Extra.DefinitionReference.class);
    switch (e.getResolved(ast()).orElse(null)) {
      case Ast.Declaration(int id) -> checkDeprecatedMaybe(node, ast().parent(id).id());
      case Ast.Enumeration(int id) -> checkDeprecatedMaybe(node, id);
      case null, default -> {
      }
    }
  }

  private void checkImplicitCastMaybe(Node<?> node, int castId) {
    if (castId > 0) {
      checkDeprecatedMaybe(node, castId);
      diagnosticImplicitCast(node, ast().node(castId));
    }
  }

  private void checkDeprecatedMaybe(Node<?> node, int targetId) {
    if (targetId == 0) return;

    var target = ast().node(targetId);
    Stream<Node<Annotation>> annotations = switch (target.data()) {
      case Klass v -> v.annotations(ast());
      case Enumeration v -> v.annotations(ast());
      case Declaration.Field v -> v.annotations(ast());
      case Declaration.Method v -> v.annotations(ast());
      case Declaration.Priority v -> v.annotations(ast());
      case Declaration.Cast v -> v.annotations(ast());
      case Declaration.Operator v -> v.annotations(ast());
      case Declaration.Records.Child v -> v.annotations(ast());
      default -> Stream.of();
    };

    annotations.filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
      .findFirst()
      .ifPresent(annotationDeprecated -> diagnosticDeprecated(node, target, annotationDeprecated));
  }
}
