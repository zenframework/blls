package org.zenframework.z8.blls.lsp;

import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

abstract class VisitorDefinition extends VisitorHit {
  protected abstract void success(Node<?> node);

  private void locationOfType(Node<? extends Expression> node) {
    switch (ast().type(node).orElse(null)) {
      case Expression.Type.Declaration v -> success(v.node(ast()));
      case Expression.Type.Enumeration v -> success(v.node(ast()));
      case null, default -> {
      }
    }
  }

  @Override
  protected void hitDeclarationFieldOperation(Node<Declaration.Field> node) {
    ast().analyzer()
      .extra(node, Extra.DeclarationField.class)
      .resolvedAssign(ast())
      .ifPresent(this::success);
  }

  @Override
  protected void hitStatementVariableOperation(Node<Statement.Variable> node) {
    ast().analyzer()
      .extra(node, Extra.StatementVariable.class)
      .resolvedAssign(ast())
      .ifPresent(this::success);
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
      case Extra.ExpressionAccess.Resolved v -> success(ast().node(v.id()));
    }
  }

  @Override
  protected void hitExpressionInvokeName(Node<Expression.Invoke> node) {
    ast().analyzer().extra(node, Extra.ExpressionInvoke.class)
      .getResolved(ast())
      .ifPresent(it -> success(ast().node(it.id())));
  }

  @Override
  protected void hitExpressionUnaryOperation(Node<Expression.Unary> node) {
    ast().analyzer()
      .extra(node, Extra.ExpressionUnary.class)
      .resolved(ast())
      .ifPresent(this::success);
  }

  @Override
  protected void hitExpressionBinaryOperation(Node<Expression.Binary> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
    e.resolved(ast()).ifPresent(this::success);
    e.resolvedAssign(ast()).ifPresent(this::success);
  }

  @Override
  protected void hitDefinitionReferenceName(Node<Definition.Reference> node) {
    switch (
      ast().analyzer()
        .extra(node, Extra.DefinitionReference.class)
        .getResolved(ast())
        .orElse(null)
    ) {
      case null -> {
      }
      case Ast.Pack _ -> {
        if (ast().parent(node.id()) instanceof Node(int id, Definition.Reference v)) {
          hitDefinitionReferenceName(new Node<>(id, v));
        }
      }
      case Ast.PackChild v -> success(ast().node(v.id()));
    }
  }
}
