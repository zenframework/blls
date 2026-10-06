package org.zenframework.z8.blls.lsp;

import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

abstract class VisitorTypeDefinition extends VisitorHit {
  protected abstract void successDeclaration(Node<Declaration> node);

  protected abstract void successEnumeration(Node<Enumeration> node);

  private void locationOfType(Node<? extends Expression> node) {
    switch (ast().type(node).orElse(null)) {
      case Expression.Type.Declaration v -> successDeclaration(v.node(ast()));
      case Expression.Type.Enumeration v -> successEnumeration(v.node(ast()));
      case null, default -> {
      }
    }
  }

  private void locationOfDefinition(Node<? extends Definition> node) {
    switch (ast().analyzer().typeOf(node).orElse(null)) {
      case Expression.Type.Declaration v -> successDeclaration(v.node(ast()));
      case Expression.Type.Enumeration v -> successEnumeration(v.node(ast()));
      case null, default -> {
      }
    }
  }

  @Override
  protected void hitParameterName(Node<Parameter> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitDeclarationFieldName(Node<Declaration.Field> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitDeclarationMethodName(Node<Declaration.Method> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitDeclarationPriorityKeyword(Node<Declaration.Priority> node) {
    successDeclaration(ast().analyzer().declarationOf(node));
  }

  @Override
  protected void hitDeclarationCastKeyword(Node<Declaration.Cast> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitDeclarationCastDefinitionReference(Node<Declaration.Cast> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitDeclarationOperatorKeyword(Node<Declaration.Operator> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitDeclarationOperatorOperation(Node<Declaration.Operator> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitDeclarationRecordsChildName(Node<Declaration.Records.Child> node) {
    locationOfType(node.data().value(ast()));
  }

  @Override
  protected void hitEnumerationChildName(Node<Enumeration.Child> node) {
    successEnumeration(ast().parent(node.id()));
  }

  @Override
  protected void hitStatementVariableName(Node<Statement.Variable> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitStatementLoopForEachName(Node<Statement.LoopForEach> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitStatementTryCatchName(Node<Statement.Try.Catch> node) {
    locationOfDefinition(node.data().definition(ast()));
  }

  @Override
  protected void hitExpressionBoolean(Node<Expression.Boolean> node) {
    locationOfType(node);
  }

  @Override
  protected void hitExpressionInteger(Node<Expression.Integer> node) {
    locationOfType(node);
  }

  @Override
  protected void hitExpressionDecimal(Node<Expression.Decimal> node) {
    locationOfType(node);
  }

  @Override
  protected void hitExpressionString(Node<Expression.String> node) {
    locationOfType(node);
  }

  @Override
  protected void hitExpressionSpecial(Node<Expression.Special> node) {
    locationOfType(node);
  }

  @Override
  protected void hitExpressionThis(Node<Expression.This> node) {
    locationOfType(node);
  }

  @Override
  protected void hitExpressionSuper(Node<Expression.Super> node) {
    locationOfType(node);
  }

  @Override
  protected void hitExpressionContainerKeyword(Node<Expression.Container> node) {
    locationOfType(node);
  }

  @Override
  protected void hitExpressionAccessName(Node<Expression.Access> node) {
    switch (
      ast().analyzer()
        .extra(node, Extra.ExpressionAccess.class)
        .getResolved(ast())
        .orElse(null)
    ) {
      case null -> {
      }
      case Extra.ExpressionAccess.Resolved.Pack _ -> {
        if (ast().parent(node.id()) instanceof Node(int id, Expression.Access v)) {
          hitExpressionAccessName(new Node<>(id, v));
        }
      }
      case Extra.ExpressionAccess.Resolved.Declaration v -> successDeclaration(v.node(ast()));
      case Extra.ExpressionAccess.Resolved.Enumeration v -> successEnumeration(v.node(ast()));
      case Extra.ExpressionAccess.Resolved.DeclarationField(int id) -> hitDeclarationFieldName(ast().node(id));
      case Extra.ExpressionAccess.Resolved.DeclarationRecordsChild(int id) ->
        hitDeclarationRecordsChildName(ast().node(id));
      case Extra.ExpressionAccess.Resolved.EnumerationChild(int id) -> hitEnumerationChildName(ast().node(id));
      case Extra.ExpressionAccess.Resolved.Parameter(int id) -> hitParameterName(ast().node(id));
      case Extra.ExpressionAccess.Resolved.StatementVariable(int id) -> hitStatementVariableName(ast().node(id));
      case Extra.ExpressionAccess.Resolved.StatementLoopForEach(int id) -> hitStatementLoopForEachName(ast().node(id));
      case Extra.ExpressionAccess.Resolved.StatementTryCatch(int id) -> hitStatementTryCatchName(ast().node(id));
    }
  }

  @Override
  protected void hitExpressionInvokeName(Node<Expression.Invoke> node) {
    switch (
      ast().analyzer()
        .extra(node, Extra.ExpressionInvoke.class)
        .getResolved(ast())
        .orElse(null)
    ) {
      case null -> {
      }
      case Extra.ExpressionInvoke.Resolved.DeclarationMethod v ->
        locationOfDefinition(v.node(ast()).data().definition(ast()));
      case Extra.ExpressionInvoke.Resolved.DeclarationCast v ->
        locationOfDefinition(v.node(ast()).data().definition(ast()));
    }
  }

  @Override
  protected void hitExpressionUnaryOperation(Node<Expression.Unary> node) {
    ast().analyzer()
      .extra(node, Extra.ExpressionUnary.class)
      .resolved(ast())
      .ifPresent(it -> locationOfDefinition(it.data().definition(ast())));
  }

  @Override
  protected void hitExpressionBinaryOperation(Node<Expression.Binary> node) {
    ast().analyzer()
      .extra(node, Extra.ExpressionBinary.class)
      .resolved(ast())
      .ifPresent(it -> locationOfDefinition(it.data().definition(ast())));
  }

  @Override
  protected void hitDefinitionReferenceName(Node<Definition.Reference> node) {
    var e = ast().analyzer().extra(node, Extra.DefinitionReference.class);
    switch (e.getResolved(ast()).orElse(null)) {
      case null -> {
      }
      case Ast.Pack _ -> {
        if (ast().parent(node.id()) instanceof Node(int id, Definition.Reference v)) {
          hitDefinitionReferenceName(new Node<>(id, v));
        }
      }
      case Ast.Declaration v -> successDeclaration(v.node(ast()));
      case Ast.Enumeration v -> successEnumeration(v.node(ast()));
    }
  }
}
