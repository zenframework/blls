package org.zenframework.z8.blls.lsp;

import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

import static org.zenframework.z8.blls.analyzer.Ast.only;

abstract class VisitorDocumentHighlight extends VisitorHit {
  protected abstract Node<Source> source();

  protected abstract void success(DocumentHighlight value);

  private boolean isLocal(Node<?> node) {
    return ast().parents(node.id())
      .reduce((_, b) -> b)
      .orElseThrow()
      == source().id();
  }

  @Override
  protected void hitKlassName(Node<Klass> node) {
    new OfDeclaration(node.data().childId()).visit(source().id());
    success(new DocumentHighlight(ast().range(ast().klassName(node))));
  }

  @Override
  protected void hitEnumerationName(Node<Enumeration> node) {
    new OfDeclaration(node.id()).visit(source().id());
    success(new DocumentHighlight(ast().range(ast().enumerationName(node))));
  }

  @Override
  protected void hitParameterName(Node<Parameter> node) {
    new OfAccess(node.id()).visit(source().id());
    success(new DocumentHighlight(ast().range(ast().parameterName(node))));
  }

  @Override
  protected void hitDeclarationFieldName(Node<Declaration.Field> node) {
    new OfAccess(node.id()).visit(source().id());
    success(new DocumentHighlight(ast().range(ast().declarationFieldName(node))));
  }

  @Override
  protected void hitDeclarationFieldOperation(Node<Declaration.Field> node) {
    ast().analyzer()
      .extra(node, Extra.DeclarationField.class)
      .resolvedAssign(ast())
      .ifPresent(it -> {
        new OfOperator(it.id()).visit(source().id());
        if (isLocal(it))
          success(new DocumentHighlight(ast().range(ast().declarationOperatorKeyword(it))));
      });
  }

  @Override
  protected void hitDeclarationMethodName(Node<Declaration.Method> node) {
    new OfMethod(node.id()).visit(source().id());
    new OfReturnThrow().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().declarationMethodName(node))));
  }

  @Override
  protected void hitDeclarationPriorityKeyword(Node<Declaration.Priority> node) {
    new OfPriority(node.id()).visit(source().id());
    new OfReturnThrow().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().declarationPriorityKeyword(node))));
  }

  @Override
  protected void hitDeclarationCastKeyword(Node<Declaration.Cast> node) {
    new OfCast(node.id()).visit(source().id());
    new OfReturnThrow().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().declarationCastKeyword(node))));
  }

  @Override
  protected void hitDeclarationCastDefinitionReference(Node<Declaration.Cast> node) {
    new OfCast(node.id()).visit(source().id());
    new OfReturnThrow().visit(node.id());
    success(new DocumentHighlight(ast().range(node.data().definition(ast()))));
  }

  @Override
  protected void hitDeclarationOperatorKeyword(Node<Declaration.Operator> node) {
    new OfOperator(node.id()).visit(source().id());
    new OfReturnThrow().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().declarationOperatorKeyword(node))));
  }

  @Override
  protected void hitDeclarationRecordsChildName(Node<Declaration.Records.Child> node) {
    new OfAccess(node.id()).visit(source().id());
    success(new DocumentHighlight(ast().range(ast().declarationRecordsChildName(node))));
  }

  @Override
  protected void hitEnumerationChildName(Node<Enumeration.Child> node) {
    new OfAccess(node.id()).visit(source().id());
    success(new DocumentHighlight(ast().range(node)));
  }

  @Override
  protected void hitStatementVariableName(Node<Statement.Variable> node) {
    new OfAccess(node.id()).visit(source().id());
    success(new DocumentHighlight(ast().range(ast().statementVariableName(node))));
  }

  @Override
  protected void hitStatementVariableOperation(Node<Statement.Variable> node) {
    ast().analyzer()
      .extra(node, Extra.StatementVariable.class)
      .resolvedAssign(ast())
      .ifPresent(it -> {
        new OfOperator(it.id()).visit(source().id());
        if (isLocal(it))
          success(new DocumentHighlight(ast().range(ast().declarationOperatorKeyword(it))));
      });
  }

  @Override
  protected void hitStatementReturnKeyword(Node<Statement.Return> node) {
    var declarationChild = ast().parents(node.id())
      .mapToObj(ast()::node)
      .gather(only(Declaration.Child.class))
      .findFirst()
      .orElseThrow();
    new OfReturnThrow().visit(declarationChild.id());
    success(new DocumentHighlight(ast().anchor(declarationChild)));
  }

  @Override
  protected void hitStatementBreakKeyword(Node<Statement.Break> node) {
    var loop = ast().parents(node.id())
      .mapToObj(ast()::node)
      .filter(it -> switch (it.data()) {
        case Statement.LoopWhile _,
             Statement.LoopFor _,
             Statement.LoopForEach _ -> true;
        default -> false;
      })
      .findFirst()
      .orElseThrow();
    success(new DocumentHighlight(ast().anchor(loop)));
    new OfLoop().visit(loop.id());
  }

  @Override
  protected void hitStatementContinueKeyword(Node<Statement.Continue> node) {
    var loop = ast().parents(node.id())
      .mapToObj(ast()::node)
      .filter(it -> switch (it.data()) {
        case Statement.LoopWhile _,
             Statement.LoopFor _,
             Statement.LoopForEach _ -> true;
        default -> false;
      })
      .findFirst()
      .orElseThrow();
    success(new DocumentHighlight(ast().anchor(loop)));
    new OfLoop().visit(loop.id());
  }

  @Override
  protected void hitStatementThrowKeyword(Node<Statement.Throw> node) {
    var declarationChild = ast().parents(node.id())
      .mapToObj(ast()::node)
      .gather(only(Declaration.Child.class))
      .findFirst()
      .orElseThrow();
    new OfReturnThrow().visit(declarationChild.id());
    success(new DocumentHighlight(ast().anchor(declarationChild)));
  }

  @Override
  protected void hitStatementLoopDoKeywordDo(Node<Statement.LoopDo> node) {
    new OfLoop().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().statementLoopDoKeywordDo(node))));
  }

  @Override
  protected void hitStatementLoopDoKeywordWhile(Node<Statement.LoopDo> node) {
    new OfLoop().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().statementLoopDoKeywordWhile(node))));
  }

  @Override
  protected void hitStatementLoopWhileKeyword(Node<Statement.LoopWhile> node) {
    new OfLoop().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().statementLoopWhileKeyword(node))));
  }

  @Override
  protected void hitStatementLoopForKeyword(Node<Statement.LoopFor> node) {
    new OfLoop().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().statementLoopForKeyword(node))));
  }

  @Override
  protected void hitStatementLoopForEachKeyword(Node<Statement.LoopForEach> node) {
    new OfLoop().visit(node.id());
    success(new DocumentHighlight(ast().range(ast().statementLoopForEachKeyword(node))));
  }

  @Override
  protected void hitStatementLoopForEachName(Node<Statement.LoopForEach> node) {
    new OfAccess(node.id()).visit(source().id());
    success(new DocumentHighlight(ast().range(ast().statementLoopForEachName(node))));
  }

  @Override
  protected void hitStatementTryCatchName(Node<Statement.Try.Catch> node) {
    new OfAccess(node.id()).visit(source().id());
    success(new DocumentHighlight(ast().range(ast().statementTryCatchName(node))));
  }

  @Override
  protected void hitExpressionThis(Node<Expression.This> node) {
    var declaration = ast().analyzer().declarationOf(node);
    new OfThisSuperContainer(declaration.id()).visit(source().id());
    success(new DocumentHighlight(ast().anchor(declaration)));
  }

  @Override
  protected void hitExpressionSuper(Node<Expression.Super> node) {
    if (ast().type(node).orElse(null) instanceof Expression.Type.Declaration v) {
      new OfThisSuperContainer(v.id()).visit(source().id());
      if (isLocal(v.node(ast())))
        success(new DocumentHighlight(ast().anchor(v.node(ast()))));
    }
  }

  @Override
  protected void hitExpressionContainerKeyword(Node<Expression.Container> node) {
    if (ast().type(node).orElse(null) instanceof Expression.Type.Declaration v) {
      new OfThisSuperContainer(v.id()).visit(source().id());
      success(new DocumentHighlight(ast().anchor(v.node(ast()))));
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
      case Extra.ExpressionAccess.Resolved.Declaration _,
           Extra.ExpressionAccess.Resolved.Enumeration _ -> {
        new OfDeclaration(r.id()).visit(source().id());
        if (isLocal(ast().node(r.id())))
          success(new DocumentHighlight(ast().anchor(ast().node(r.id()))));
      }
      case Extra.ExpressionAccess.Resolved.DeclarationField _,
           Extra.ExpressionAccess.Resolved.DeclarationRecordsChild _,
           Extra.ExpressionAccess.Resolved.EnumerationChild _,
           Extra.ExpressionAccess.Resolved.Parameter _,
           Extra.ExpressionAccess.Resolved.StatementVariable _ -> {
        new OfAccess(r.id()).visit(source().id());
        if (isLocal(ast().node(r.id())))
          success(new DocumentHighlight(ast().anchor(ast().node(r.id()))));
      }
      case Extra.ExpressionAccess.Resolved.StatementLoopForEach _ -> {
        new OfAccess(r.id()).visit(source().id());
        success(new DocumentHighlight(ast().range(ast().statementLoopForEachName(ast().node(r.id())))));
      }
      case Extra.ExpressionAccess.Resolved.StatementTryCatch _ -> {
        new OfAccess(r.id()).visit(source().id());
        success(new DocumentHighlight(ast().range(ast().statementTryCatchName(ast().node(r.id())))));
      }
    }
  }

  @Override
  protected void hitExpressionInvokeName(Node<Expression.Invoke> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionInvoke.class);
    if (e.getResolved(ast()).orElse(null) instanceof Extra.ExpressionInvoke.Resolved.DeclarationMethod v) {
      new OfMethod(v.id()).visit(source().id());
      if (isLocal(v.node(ast())))
        success(new DocumentHighlight(ast().range(ast().declarationMethodName(v.node(ast())))));
    }
  }

  @Override
  protected void hitExpressionUnaryOperation(Node<Expression.Unary> node) {
    ast().analyzer()
      .extra(node, Extra.ExpressionUnary.class)
      .resolved(ast())
      .ifPresent(it -> {
        new OfOperator(it.id()).visit(source().id());
        if (isLocal(it))
          success(new DocumentHighlight(ast().range(ast().declarationOperatorKeyword(it))));
      });
  }

  @Override
  protected void hitExpressionBinaryOperation(Node<Expression.Binary> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
    e.resolved(ast()).ifPresent(it -> {
      new OfOperator(it.id()).visit(source().id());
      if (isLocal(it))
        success(new DocumentHighlight(ast().range(ast().declarationOperatorKeyword(it))));
    });
    e.resolvedAssign(ast()).ifPresent(it -> {
      new OfOperator(it.id()).visit(source().id());
      if (isLocal(it))
        success(new DocumentHighlight(ast().range(ast().declarationOperatorKeyword(it))));
    });
  }

  @Override
  protected void hitDefinitionReferenceName(Node<Definition.Reference> node) {
    var r = ast().analyzer()
      .extra(node, Extra.DefinitionReference.class)
      .getResolved(ast())
      .orElse(null);
    switch (r) {
      case null -> {
      }
      case Ast.Pack _ -> {
      }
      case Ast.Declaration _,
           Ast.Enumeration _ -> {
        new OfDeclaration(r.id()).visit(source().id());
        if (isLocal(ast().node(r.id())))
          success(new DocumentHighlight(ast().anchor(ast().node(r.id()))));
      }
    }
  }

  private final class OfAccess extends Visitor {
    private final int targetId;

    private OfAccess(int targetId) {
      this.targetId = targetId;
    }

    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
    }

    @Override
    protected void expressionAccess(Node<Expression.Access> node) {
      super.expressionAccess(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionAccess.class);
      if (e.resolvedId == targetId) {
        success(new DocumentHighlight(
          ast().range(ast().expressionAccessName(node)),
          isWrite(node) ? DocumentHighlightKind.Write : DocumentHighlightKind.Read
        ));
      }
    }

    private boolean isWrite(Node<? extends Expression> node) {
      return switch (ast().parent(node.id())) {
        case Node(int id, Expression.Index v) -> v.childId() == node.id() &&
          !(ast().parent(id).data() instanceof Expression.Index) &&
          isWrite(new Node<>(id, v));
        case Node(int _, Expression.Binary v) -> switch (v.operation()) {
          case Operation.ASSIGN,
               Operation.ASSIGN_ADD,
               Operation.ASSIGN_SUBTRACT,
               Operation.ASSIGN_MULTIPLY,
               Operation.ASSIGN_DIVIDE,
               Operation.ASSIGN_MODULE -> v.leftId() == node.id();
          default -> false;
        };
        default -> false;
      };
    }
  }

  private final class OfDeclaration extends Visitor {
    private final int targetId;

    private OfDeclaration(int targetId) {
      this.targetId = targetId;
    }

    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
    }

    @Override
    protected void declaration(Node<Declaration> node) {
      super.declaration(node);

      ast().analyzer()
        .inheritance(node)
        .filter(it -> it.id() == targetId)
        .findFirst()
        .ifPresent(_ -> success(new DocumentHighlight(ast().anchor(node))));
    }

    @Override
    protected void expressionAccess(Node<Expression.Access> node) {
      super.expressionAccess(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionAccess.class);
      if (e.resolvedId == targetId) {
        success(new DocumentHighlight(ast().range(ast().expressionAccessName(node))));
      }
    }

    @Override
    protected void definitionReference(Node<Definition.Reference> node) {
      super.definitionReference(node);

      var e = ast().analyzer().extra(node, Extra.DefinitionReference.class);
      if (e.resolvedId == targetId) {
        success(new DocumentHighlight(ast().range(ast().definitionReferenceName(node))));
      }
    }
  }

  private final class OfThisSuperContainer extends Visitor {
    private final int targetId;

    private OfThisSuperContainer(int targetId) {
      this.targetId = targetId;
    }

    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
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
        .filter(it -> it instanceof Expression.Type.Declaration(int id) && id == targetId)
        .ifPresent(_ -> success(new DocumentHighlight(ast().anchor(node))));
    }
  }

  private final class OfMethod extends Visitor {
    private final int targetId;

    private OfMethod(int targetId) {
      this.targetId = targetId;
    }

    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
    }

    @Override
    protected void declarationMethod(Node<Declaration.Method> node) {
      super.declarationMethod(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationMethod.class);
      if (e.overrideId == targetId) {
        success(new DocumentHighlight(ast().range(ast().declarationMethodName(node))));
      }
    }

    @Override
    protected void expressionInvoke(Node<Expression.Invoke> node) {
      super.expressionInvoke(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionInvoke.class);
      if (e.resolvedId == targetId) {
        success(new DocumentHighlight(ast().range(ast().expressionInvokeName(node))));
      }
    }
  }

  private final class OfPriority extends Visitor {
    private final int targetId;

    private OfPriority(int targetId) {
      this.targetId = targetId;
    }

    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
    }

    @Override
    protected void declarationPriority(Node<Declaration.Priority> node) {
      super.declarationPriority(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationPriority.class);
      if (e.overrideId == targetId) {
        success(new DocumentHighlight(ast().range(ast().declarationPriorityKeyword(node))));
      }
    }

    @Override
    protected void statementReturn(Node<Statement.Return> node) {
      super.statementReturn(node);

      var e = ast().analyzer().extra(node, Extra.StatementReturn.class);
      if (e.resolvedPriorityId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()).orElseThrow())));
      }
    }

    @Override
    protected void expressionParen(Node<Expression.Paren> node) {
      super.expressionParen(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionParen.class);
      if (e.resolvedPriorityId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().child(ast()))));
      }
    }
  }

  private final class OfCast extends Visitor {
    private final int targetId;

    private OfCast(int targetId) {
      this.targetId = targetId;
    }

    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
    }

    @Override
    protected void declarationField(Node<Declaration.Field> node) {
      super.declarationField(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationField.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()).orElseThrow())));
      }
    }

    @Override
    protected void declarationCast(Node<Declaration.Cast> node) {
      super.declarationCast(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationCast.class);
      if (e.overrideId == targetId) {
        success(new DocumentHighlight(ast().range(ast().declarationCastKeyword(node))));
      }
    }

    @Override
    protected void statementVariable(Node<Statement.Variable> node) {
      super.statementVariable(node);

      var e = ast().analyzer().extra(node, Extra.StatementVariable.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()).orElseThrow())));
      }
    }

    @Override
    protected void statementReturn(Node<Statement.Return> node) {
      super.statementReturn(node);

      var e = ast().analyzer().extra(node, Extra.StatementReturn.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()).orElseThrow())));
      }
    }

    @Override
    protected void statementThrow(Node<Statement.Throw> node) {
      super.statementThrow(node);

      var e = ast().analyzer().extra(node, Extra.StatementThrow.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()))));
      }
    }

    @Override
    protected void statementCondition(Node<Statement.Condition> node) {
      super.statementCondition(node);

      var e = ast().analyzer().extra(node, Extra.StatementCondition.class);
      if (e.conditionCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().condition(ast()))));
      }
    }

    @Override
    protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
      super.statementLoopWhile(node);

      var e = ast().analyzer().extra(node, Extra.StatementLoopWhile.class);
      if (e.conditionCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().condition(ast()))));
      }
    }

    @Override
    protected void statementLoopFor(Node<Statement.LoopFor> node) {
      super.statementLoopFor(node);

      var e = ast().analyzer().extra(node, Extra.StatementLoopFor.class);
      if (e.conditionCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().condition(ast()))));
      }
    }

    @Override
    protected void expressionIndex(Node<Expression.Index> node) {
      super.expressionIndex(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionIndex.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()))));
      }
    }

    @Override
    protected void expressionCast(Node<Expression.Cast> node) {
      super.expressionCast(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionCast.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()))));
      }
    }

    @Override
    protected void expressionUnary(Node<Expression.Unary> node) {
      super.expressionUnary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionUnary.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()))));
      }
    }

    @Override
    protected void expressionBinary(Node<Expression.Binary> node) {
      super.expressionBinary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
      if (e.leftCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().left(ast()))));
      }
      if (e.rightCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().right(ast()))));
      }
      if (e.rightCastAssignId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().right(ast()))));
      }
    }

    @Override
    protected void expressionTernary(Node<Expression.Ternary> node) {
      super.expressionTernary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionTernary.class);
      if (e.conditionCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().condition(ast()))));
      }
      if (e.thenCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().then(ast()))));
      }
      if (e.otherCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().other(ast()))));
      }
    }

    @Override
    protected void expressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
      super.expressionArrayChildValue(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionArrayChildValue.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()))));
      }
    }

    @Override
    protected void expressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
      super.expressionArrayChildEntry(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionArrayChildEntry.class);
      if (e.keyCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().key(ast()))));
      }
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()))));
      }
    }

    @Override
    protected void expressionInvokeArgument(Node<Expression.Invoke.Argument> node) {
      super.expressionInvokeArgument(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionInvokeArgument.class);
      if (e.valueCastId == targetId) {
        success(new DocumentHighlight(ast().range(node.data().value(ast()))));
      }
    }
  }

  private final class OfOperator extends Visitor {
    private final int targetId;

    private OfOperator(int targetId) {
      this.targetId = targetId;
    }

    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
    }

    @Override
    protected void declarationField(Node<Declaration.Field> node) {
      super.declarationField(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationField.class);
      if (e.resolvedAssignId == targetId) {
        success(new DocumentHighlight(ast().range(ast().declarationFieldOperation(node).orElseThrow())));
      }
    }

    @Override
    protected void declarationOperator(Node<Declaration.Operator> node) {
      super.declarationOperator(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationOperator.class);
      if (e.overrideId == targetId) {
        success(new DocumentHighlight(ast().range(ast().declarationOperatorKeyword(node))));
      }
    }

    @Override
    protected void statementVariable(Node<Statement.Variable> node) {
      super.statementVariable(node);

      var e = ast().analyzer().extra(node, Extra.StatementVariable.class);
      if (e.resolvedAssignId == targetId) {
        success(new DocumentHighlight(ast().range(ast().statementVariableOperation(node).orElseThrow())));
      }
    }

    @Override
    protected void expressionUnary(Node<Expression.Unary> node) {
      super.expressionUnary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionUnary.class);
      if (e.resolvedId == targetId) {
        success(new DocumentHighlight(ast().range(ast().expressionUnaryOperation(node))));
      }
    }

    @Override
    protected void expressionBinary(Node<Expression.Binary> node) {
      super.expressionBinary(node);

      var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
      if (e.resolvedId == targetId || e.resolvedAssignId == targetId) {
        success(new DocumentHighlight(ast().range(ast().expressionBinaryOperation(node))));
      }
    }
  }

  private final class OfReturnThrow extends Visitor {
    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
    }

    @Override
    protected void statementReturn(Node<Statement.Return> node) {
      super.statementReturn(node);

      success(new DocumentHighlight(ast().range(ast().statementReturnKeyword(node))));
    }

    @Override
    protected void statementThrow(Node<Statement.Throw> node) {
      super.statementThrow(node);

      success(new DocumentHighlight(ast().range(ast().statementThrowKeyword(node))));
    }
  }

  private final class OfLoop extends Visitor {
    @Override
    protected Ast ast() {
      return VisitorDocumentHighlight.this.ast();
    }

    @Override
    protected void statementReturn(Node<Statement.Return> node) {
      super.statementReturn(node);

      success(new DocumentHighlight(ast().range(ast().statementReturnKeyword(node))));
    }

    @Override
    protected void statementBreak(Node<Statement.Break> node) {
      super.statementBreak(node);

      success(new DocumentHighlight(ast().range(node)));
    }

    @Override
    protected void statementContinue(Node<Statement.Continue> node) {
      super.statementContinue(node);

      success(new DocumentHighlight(ast().range(node)));
    }

    @Override
    protected void statementThrow(Node<Statement.Throw> node) {
      super.statementThrow(node);

      success(new DocumentHighlight(ast().range(ast().statementThrowKeyword(node))));
    }

    @Override
    protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
    }

    @Override
    protected void statementLoopFor(Node<Statement.LoopFor> node) {
    }

    @Override
    protected void statementLoopForEach(Node<Statement.LoopForEach> node) {
    }
  }
}
