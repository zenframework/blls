package org.zenframework.z8.blls.lsp;

import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

abstract class VisitorReferences extends VisitorHit {
  protected abstract void success(Node<?> node);

  protected abstract void visitGlobal(VisitorGlobal visitor);

  @Override
  protected void hitKlassName(Node<Klass> node) {
    visitGlobal(new VisitorGlobalDeclaration(node.data().child(ast())));
  }

  @Override
  protected void hitEnumerationName(Node<Enumeration> node) {
    visitGlobal(new VisitorGlobalDeclaration(node));
  }

  @Override
  protected void hitParameterName(Node<Parameter> node) {
    // search variable usage (not global actually)
    new VisitorGlobalAccess(node).visit(ast().parent(node.id()).id());
  }

  @Override
  protected void hitDeclarationFieldName(Node<Declaration.Field> node) {
    visitGlobal(new VisitorGlobalAccess(node));
  }

  @Override
  protected void hitDeclarationFieldOperation(Node<Declaration.Field> node) {
    ast().analyzer()
      .extra(node, Extra.DeclarationField.class)
      .resolvedAssign(ast())
      .ifPresent(it -> {
        visitGlobal(new VisitorGlobalOperator(it));
        success(it);
      });
  }

  @Override
  protected void hitDeclarationMethodName(Node<Declaration.Method> node) {
    visitGlobal(new VisitorGlobalMethod(node));
  }

  @Override
  protected void hitDeclarationPriorityKeyword(Node<Declaration.Priority> node) {
    visitGlobal(new VisitorGlobalPriority(node));
  }

  @Override
  protected void hitDeclarationCastKeyword(Node<Declaration.Cast> node) {
    visitGlobal(new VisitorGlobalCast(node));
  }

  @Override
  protected void hitDeclarationCastDefinitionReference(Node<Declaration.Cast> node) {
    hitDeclarationCastKeyword(node);
  }

  @Override
  protected void hitDeclarationOperatorKeyword(Node<Declaration.Operator> node) {
    visitGlobal(new VisitorGlobalOperator(node));
  }

  @Override
  protected void hitDeclarationOperatorOperation(Node<Declaration.Operator> node) {
    visitGlobal(new VisitorGlobalOperator(node));
  }

  @Override
  protected void hitDeclarationRecordsChildName(Node<Declaration.Records.Child> node) {
    visitGlobal(new VisitorGlobalAccess(node));
  }

  @Override
  protected void hitEnumerationChildName(Node<Enumeration.Child> node) {
    visitGlobal(new VisitorGlobalAccess(node));
  }

  @Override
  protected void hitStatementVariableName(Node<Statement.Variable> node) {
    // search variable usage in Statement.Block or Statement.LoopFor (not global actually)
    new VisitorGlobalAccess(node).visit(ast().parent(node.id()).id());
  }

  @Override
  protected void hitStatementVariableOperation(Node<Statement.Variable> node) {
    ast().analyzer()
      .extra(node, Extra.StatementVariable.class)
      .resolvedAssign(ast())
      .ifPresent(it -> {
        visitGlobal(new VisitorGlobalOperator(it));
        success(it);
      });
  }

  @Override
  protected void hitStatementLoopForEachName(Node<Statement.LoopForEach> node) {
    // search variable usage (not global actually)
    new VisitorGlobalAccess(node).visit(node.data().statementId());
  }

  @Override
  protected void hitStatementTryCatchName(Node<Statement.Try.Catch> node) {
    // search variable usage (not global actually)
    new VisitorGlobalAccess(node).visit(node.data().blockId());
  }

  @Override
  protected void hitExpressionThis(Node<Expression.This> node) {
    if (ast().type(node).orElse(null) instanceof Expression.Type.Declaration v) {
      visitGlobal(new VisitorGlobalDeclaration(v.node(ast())));
    }
  }

  @Override
  protected void hitExpressionSuper(Node<Expression.Super> node) {
    if (ast().type(node).orElse(null) instanceof Expression.Type.Declaration v) {
      visitGlobal(new VisitorGlobalDeclaration(v.node(ast())));
    }
  }

  @Override
  protected void hitExpressionContainerKeyword(Node<Expression.Container> node) {
    if (ast().type(node).orElse(null) instanceof Expression.Type.Declaration v) {
      visitGlobal(new VisitorGlobalDeclaration(v.node(ast())));
    }
  }

  @Override
  protected void hitExpressionAccessName(Node<Expression.Access> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionAccess.class);
    var r = e.getResolved(ast()).orElse(null);
    switch (r) {
      case null -> {
      }
      case Extra.ExpressionAccess.Resolved.Pack _ -> {
        if (ast().parent(node.id()) instanceof Node(int id, Expression.Access v)) {
          hitExpressionAccessName(new Node<>(id, v));
        }
      }
      case Extra.ExpressionAccess.Resolved.Declaration v -> visitGlobal(new VisitorGlobalDeclaration(v.node(ast())));
      case Extra.ExpressionAccess.Resolved.Enumeration v -> visitGlobal(new VisitorGlobalDeclaration(v.node(ast())));
      case Extra.ExpressionAccess.Resolved.DeclarationField _,
           Extra.ExpressionAccess.Resolved.DeclarationRecordsChild _,
           Extra.ExpressionAccess.Resolved.EnumerationChild _ ->
        visitGlobal(new VisitorGlobalAccess(ast().node(r.id())));
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
      case Extra.ExpressionInvoke.Resolved.DeclarationMethod v -> hitDeclarationMethodName(v.node(ast()));
      case Extra.ExpressionInvoke.Resolved.DeclarationCast v -> hitDeclarationCastKeyword(v.node(ast()));
    }
  }

  @Override
  protected void hitExpressionUnaryOperation(Node<Expression.Unary> node) {
    ast().analyzer()
      .extra(node, Extra.ExpressionUnary.class)
      .resolved(ast())
      .ifPresent(it -> {
        visitGlobal(new VisitorGlobalOperator(it));
        success(it);
      });
  }

  @Override
  protected void hitExpressionBinaryOperation(Node<Expression.Binary> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
    e.resolved(ast()).ifPresent(it -> {
      visitGlobal(new VisitorGlobalOperator(it));
      success(it);
    });
    e.resolved(ast()).ifPresent(it -> {
      visitGlobal(new VisitorGlobalOperator(it));
      success(it);
    });
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
      case Ast.PackChild v -> visitGlobal(new VisitorGlobalDeclaration(ast().node(v.id())));
    }
  }

  sealed abstract static class VisitorGlobal extends Visitor {
    final Node<?> target;

    protected VisitorGlobal(Node<?> target) {
      this.target = target;
    }
  }

  private final class VisitorGlobalAccess extends VisitorGlobal {
    private VisitorGlobalAccess(Node<?> target) {
      super(target);
    }

    @Override
    protected Ast ast() {
      return VisitorReferences.this.ast();
    }

    @Override
    protected void expressionAccess(Node<Expression.Access> node) {
      super.expressionAccess(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionAccess.class);
      if (e.resolvedId == target.id()) {
        success(node);
      }
    }
  }

  private final class VisitorGlobalDeclaration extends VisitorGlobal {
    private VisitorGlobalDeclaration(Node<?> target) {
      super(target);
    }

    @Override
    protected Ast ast() {
      return VisitorReferences.this.ast();
    }

    @Override
    protected void declaration(Node<Declaration> node) {
      super.declaration(node);

      ast().analyzer()
        .inheritance(node)
        .skip(1)
        .findFirst()
        .filter(it -> it.id() == target.id())
        .ifPresent(_ -> success(node));
    }

    @Override
    protected void expressionThis(Node<Expression.This> node) {
      super.expressionThis(node);
      run(node);
    }

    @Override
    protected void expressionSuper(Node<Expression.Super> node) {
      super.expressionSuper(node);
      run(node);
    }

    @Override
    protected void expressionContainer(Node<Expression.Container> node) {
      super.expressionContainer(node);
      run(node);
    }

    private void run(Node<? extends Expression> node) {
      ast().type(node)
        .filter(it -> it instanceof Expression.Type.Declaration(int id) && id == target.id())
        .ifPresent(_ -> success(node));
    }

    @Override
    protected void expressionAccess(Node<Expression.Access> node) {
      super.expressionAccess(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionAccess.class);
      if (e.resolvedId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void definitionReference(Node<Definition.Reference> node) {
      super.definitionReference(node);

      var e = ast().analyzer().extra(node, Extra.DefinitionReference.class);
      if (e.resolvedId == target.id()) {
        success(node);
      }
    }
  }

  private final class VisitorGlobalMethod extends VisitorGlobal {
    private VisitorGlobalMethod(Node<Declaration.Method> target) {
      super(target);
    }

    @Override
    protected Ast ast() {
      return VisitorReferences.this.ast();
    }

    @Override
    protected void declarationMethod(Node<Declaration.Method> node) {
      super.declarationMethod(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationMethod.class);
      if (e.overrideId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void expressionInvoke(Node<Expression.Invoke> node) {
      super.expressionInvoke(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionInvoke.class);
      if (e.resolvedId == target.id()) {
        success(node);
      }
    }
  }

  private final class VisitorGlobalPriority extends VisitorGlobal {
    private VisitorGlobalPriority(Node<Declaration.Priority> target) {
      super(target);
    }

    @Override
    protected Ast ast() {
      return VisitorReferences.this.ast();
    }

    @Override
    protected void declarationPriority(Node<Declaration.Priority> node) {
      super.declarationPriority(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationPriority.class);
      if (e.overrideId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void statementReturn(Node<Statement.Return> node) {
      super.statementReturn(node);

      var e = ast().analyzer().extra(node, Extra.StatementReturn.class);
      if (e.resolvedPriorityId == target.id()) {
        success(node.data().value(ast()).orElseThrow());
      }
    }

    @Override
    protected void expressionParen(Node<Expression.Paren> node) {
      super.expressionParen(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionParen.class);
      if (e.resolvedPriorityId == target.id()) {
        success(node.data().child(ast()));
      }
    }
  }

  private final class VisitorGlobalCast extends VisitorGlobal {
    private VisitorGlobalCast(Node<Declaration.Cast> target) {
      super(target);
    }

    @Override
    protected Ast ast() {
      return VisitorReferences.this.ast();
    }

    @Override
    protected void declarationField(Node<Declaration.Field> node) {
      super.declarationField(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationField.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()).orElseThrow());
      }
    }

    @Override
    protected void declarationCast(Node<Declaration.Cast> node) {
      super.declarationCast(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationCast.class);
      if (e.overrideId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void statementVariable(Node<Statement.Variable> node) {
      super.statementVariable(node);

      var e = ast().analyzer().extra(node, Extra.StatementVariable.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()).orElseThrow());
      }
    }

    @Override
    protected void statementReturn(Node<Statement.Return> node) {
      super.statementReturn(node);

      var e = ast().analyzer().extra(node, Extra.StatementReturn.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()).orElseThrow());
      }
    }

    @Override
    protected void statementThrow(Node<Statement.Throw> node) {
      super.statementThrow(node);

      var e = ast().analyzer().extra(node, Extra.StatementThrow.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()));
      }
    }

    @Override
    protected void statementCondition(Node<Statement.Condition> node) {
      super.statementCondition(node);

      var e = ast().analyzer().extra(node, Extra.StatementCondition.class);
      if (e.conditionCastId == target.id()) {
        success(node.data().condition(ast()));
      }
    }

    @Override
    protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
      super.statementLoopWhile(node);

      var e = ast().analyzer().extra(node, Extra.StatementLoopWhile.class);
      if (e.conditionCastId == target.id()) {
        success(node.data().condition(ast()));
      }
    }

    @Override
    protected void statementLoopFor(Node<Statement.LoopFor> node) {
      super.statementLoopFor(node);

      var e = ast().analyzer().extra(node, Extra.StatementLoopFor.class);
      if (e.conditionCastId == target.id()) {
        success(node.data().condition(ast()));
      }
    }

    @Override
    protected void expressionIndex(Node<Expression.Index> node) {
      super.expressionIndex(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionIndex.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()));
      }
    }

    @Override
    protected void expressionCast(Node<Expression.Cast> node) {
      super.expressionCast(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionCast.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()));
      }
    }

    @Override
    protected void expressionUnary(Node<Expression.Unary> node) {
      super.expressionUnary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionUnary.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()));
      }
    }

    @Override
    protected void expressionBinary(Node<Expression.Binary> node) {
      super.expressionBinary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
      if (e.leftCastId == target.id()) {
        success(node.data().left(ast()));
      }
      if (e.rightCastId == target.id()) {
        success(node.data().right(ast()));
      }
      if (e.rightCastAssignId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void expressionTernary(Node<Expression.Ternary> node) {
      super.expressionTernary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionTernary.class);
      if (e.conditionCastId == target.id()) {
        success(node.data().condition(ast()));
      }
      if (e.thenCastId == target.id()) {
        success(node.data().then(ast()));
      }
      if (e.otherCastId == target.id()) {
        success(node.data().other(ast()));
      }
    }

    @Override
    protected void expressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
      super.expressionArrayChildValue(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionArrayChildValue.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()));
      }
    }

    @Override
    protected void expressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
      super.expressionArrayChildEntry(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionArrayChildEntry.class);
      if (e.keyCastId == target.id()) {
        success(node.data().key(ast()));
      }
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()));
      }
    }

    @Override
    protected void expressionInvokeArgument(Node<Expression.Invoke.Argument> node) {
      super.expressionInvokeArgument(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionInvokeArgument.class);
      if (e.valueCastId == target.id()) {
        success(node.data().value(ast()));
      }
    }
  }

  private final class VisitorGlobalOperator extends VisitorGlobal {
    private VisitorGlobalOperator(Node<Declaration.Operator> target) {
      super(target);
    }

    @Override
    protected Ast ast() {
      return VisitorReferences.this.ast();
    }

    @Override
    protected void declarationField(Node<Declaration.Field> node) {
      super.declarationField(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationField.class);
      if (e.resolvedAssignId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void declarationOperator(Node<Declaration.Operator> node) {
      super.declarationOperator(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationOperator.class);
      if (e.overrideId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void statementVariable(Node<Statement.Variable> node) {
      super.statementVariable(node);

      var e = ast().analyzer().extra(node, Extra.StatementVariable.class);
      if (e.resolvedAssignId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void expressionUnary(Node<Expression.Unary> node) {
      super.expressionUnary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionUnary.class);
      if (e.resolvedId == target.id()) {
        success(node);
      }
    }

    @Override
    protected void expressionBinary(Node<Expression.Binary> node) {
      super.expressionBinary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
      if (e.resolvedId == target.id() || e.resolvedAssignId == target.id()) {
        success(node);
      }
    }
  }
}
