package org.zenframework.z8.blls.lsp;

import org.eclipse.lsp4j.Position;
import org.zenframework.z8.blls.ast.*;

abstract class VisitorHit extends Visitor {
  @Override
  protected abstract Ast ast();

  protected abstract Position position();

  protected void hitKlassName(Node<Klass> node) {
  }

  protected void hitEnumerationName(Node<Enumeration> node) {
  }

  protected void hitAnnotationName(Node<Annotation> node) {
  }

  protected void hitParameterName(Node<Parameter> node) {
  }

  protected void hitDeclarationFieldName(Node<Declaration.Field> node) {
  }

  protected void hitDeclarationFieldOperation(Node<Declaration.Field> node) {
  }

  protected void hitDeclarationMethodName(Node<Declaration.Method> node) {
  }

  protected void hitDeclarationPriorityKeyword(Node<Declaration.Priority> node) {
  }

  protected void hitDeclarationCastKeyword(Node<Declaration.Cast> node) {
  }

  protected void hitDeclarationCastDefinitionReference(Node<Declaration.Cast> node) {
  }

  protected void hitDeclarationOperatorKeyword(Node<Declaration.Operator> node) {
  }

  protected void hitDeclarationOperatorOperation(Node<Declaration.Operator> node) {
  }

  protected void hitDeclarationRecordsKeyword(Node<Declaration.Records> node) {
  }

  protected void hitDeclarationRecordsChildName(Node<Declaration.Records.Child> node) {
  }

  protected void hitEnumerationChildName(Node<Enumeration.Child> node) {
  }

  protected void hitStatementVariableName(Node<Statement.Variable> node) {
  }

  protected void hitStatementVariableOperation(Node<Statement.Variable> node) {
  }

  protected void hitStatementReturnKeyword(Node<Statement.Return> node) {
  }

  protected void hitStatementBreakKeyword(Node<Statement.Break> node) {
  }

  protected void hitStatementContinueKeyword(Node<Statement.Continue> node) {
  }

  protected void hitStatementThrowKeyword(Node<Statement.Throw> node) {
  }

  protected void hitStatementTryKeyword(Node<Statement.Try> node) {
  }

  protected void hitStatementConditionKeyword(Node<Statement.Condition> node) {
  }

  protected void hitStatementLoopDoKeywordDo(Node<Statement.LoopDo> node) {
  }

  protected void hitStatementLoopDoKeywordWhile(Node<Statement.LoopDo> node) {
  }

  protected void hitStatementLoopWhileKeyword(Node<Statement.LoopWhile> node) {
  }

  protected void hitStatementLoopForKeyword(Node<Statement.LoopFor> node) {
  }

  protected void hitStatementLoopForEachKeyword(Node<Statement.LoopForEach> node) {
  }

  protected void hitStatementLoopForEachName(Node<Statement.LoopForEach> node) {
  }

  protected void hitStatementTryCatchName(Node<Statement.Try.Catch> node) {
  }

  protected void hitExpressionNull(Node<Expression.Null> node) {
  }

  protected void hitExpressionBoolean(Node<Expression.Boolean> node) {
  }

  protected void hitExpressionInteger(Node<Expression.Integer> node) {
  }

  protected void hitExpressionDecimal(Node<Expression.Decimal> node) {
  }

  protected void hitExpressionString(Node<Expression.String> node) {
  }

  protected void hitExpressionSpecial(Node<Expression.Special> node) {
  }

  protected void hitExpressionThis(Node<Expression.This> node) {
  }

  protected void hitExpressionSuper(Node<Expression.Super> node) {
  }

  protected void hitExpressionContainerKeyword(Node<Expression.Container> node) {
  }

  protected void hitExpressionAccessName(Node<Expression.Access> node) {
  }

  protected void hitExpressionInvokeName(Node<Expression.Invoke> node) {
  }

  protected void hitExpressionObjectKeyword(Node<Expression.Object> node) {
  }

  protected void hitExpressionUnaryOperation(Node<Expression.Unary> node) {
  }

  protected void hitExpressionBinaryOperation(Node<Expression.Binary> node) {
  }

  protected void hitExpressionInstanceofKeyword(Node<Expression.Instanceof> node) {
  }

  protected void hitDefinitionReferenceName(Node<Definition.Reference> node) {
  }

  @Override
  public void visit(int id) {
    if (Ast.hits(ast().range(ast().node(id)), position()))
      super.visit(id);
  }

  @Override
  protected void klass(Node<Klass> node) {
    if (Ast.hits(ast().range(ast().klassName(node)), position()))
      hitKlassName(node);
    else
      super.klass(node);
  }

  @Override
  protected void enumeration(Node<Enumeration> node) {
    if (Ast.hits(ast().range(ast().enumerationName(node)), position()))
      hitEnumerationName(node);
    else
      super.enumeration(node);
  }

  @Override
  protected void annotation(Node<Annotation> node) {
    if (Ast.hits(ast().range(ast().annotationName(node)), position()))
      hitAnnotationName(node);
    else
      super.annotation(node);
  }

  @Override
  protected void parameter(Node<Parameter> node) {
    if (Ast.hits(ast().range(ast().parameterName(node)), position()))
      hitParameterName(node);
    else
      super.parameter(node);
  }

  @Override
  protected void declarationField(Node<Declaration.Field> node) {
    if (Ast.hits(ast().range(ast().declarationFieldName(node)), position()))
      hitDeclarationFieldName(node);
    else if (ast().declarationFieldOperation(node)
      .map(ast()::range)
      .filter(it -> Ast.hits(it, position()))
      .isPresent())
      hitDeclarationFieldOperation(node);
    else
      super.declarationField(node);
  }

  @Override
  protected void declarationMethod(Node<Declaration.Method> node) {
    if (Ast.hits(ast().range(ast().declarationMethodName(node)), position()))
      hitDeclarationMethodName(node);
    else
      super.declarationMethod(node);
  }

  @Override
  protected void declarationPriority(Node<Declaration.Priority> node) {
    if (Ast.hits(ast().range(ast().declarationPriorityKeyword(node)), position()))
      hitDeclarationPriorityKeyword(node);
    else
      super.declarationPriority(node);
  }

  @Override
  protected void declarationCast(Node<Declaration.Cast> node) {
    if (Ast.hits(ast().range(ast().declarationCastKeyword(node)), position()))
      hitDeclarationCastKeyword(node);
    if (Ast.hits(ast().range(node.data().definition(ast())), position()))
      hitDeclarationCastDefinitionReference(node);
    else
      super.declarationCast(node);
  }

  @Override
  protected void declarationOperator(Node<Declaration.Operator> node) {
    if (Ast.hits(ast().range(ast().declarationOperatorKeyword(node)), position()))
      hitDeclarationOperatorKeyword(node);
    else if (Ast.hits(ast().range(ast().declarationOperatorOperation(node)), position()))
      hitDeclarationOperatorOperation(node);
    else
      super.declarationOperator(node);
  }

  @Override
  protected void declarationRecords(Node<Declaration.Records> node) {
    if (Ast.hits(ast().range(ast().declarationRecordsKeyword(node)), position()))
      hitDeclarationRecordsKeyword(node);
    else
      super.declarationRecords(node);
  }

  @Override
  protected void declarationRecordsChild(Node<Declaration.Records.Child> node) {
    if (Ast.hits(ast().range(ast().declarationRecordsChildName(node)), position()))
      hitDeclarationRecordsChildName(node);
    else
      super.declarationRecordsChild(node);
  }

  @Override
  protected void enumerationChild(Node<Enumeration.Child> node) {
    if (Ast.hits(ast().range(ast().enumerationChildName(node)), position()))
      hitEnumerationChildName(node);
    else
      super.enumerationChild(node);
  }

  @Override
  protected void statementVariable(Node<Statement.Variable> node) {
    if (Ast.hits(ast().range(ast().statementVariableName(node)), position()))
      hitStatementVariableName(node);
    else if (ast().statementVariableOperation(node)
      .map(ast()::range)
      .filter(it -> Ast.hits(it, position()))
      .isPresent())
      hitStatementVariableOperation(node);
    else
      super.statementVariable(node);
  }

  @Override
  protected void statementReturn(Node<Statement.Return> node) {
    if (Ast.hits(ast().range(ast().statementReturnKeyword(node)), position()))
      hitStatementReturnKeyword(node);
    else
      super.statementReturn(node);
  }

  @Override
  protected void statementBreak(Node<Statement.Break> node) {
    if (Ast.hits(ast().range(node), position()))
      hitStatementBreakKeyword(node);
    else
      super.statementBreak(node);
  }

  @Override
  protected void statementContinue(Node<Statement.Continue> node) {
    if (Ast.hits(ast().range(node), position()))
      hitStatementContinueKeyword(node);
    else
      super.statementContinue(node);
  }

  @Override
  protected void statementThrow(Node<Statement.Throw> node) {
    if (Ast.hits(ast().range(ast().statementThrowKeyword(node)), position()))
      hitStatementThrowKeyword(node);
    else
      super.statementThrow(node);
  }

  @Override
  protected void statementTry(Node<Statement.Try> node) {
    if (Ast.hits(ast().range(ast().statementTryKeyword(node)), position()))
      hitStatementTryKeyword(node);
    else
      super.statementTry(node);
  }

  @Override
  protected void statementCondition(Node<Statement.Condition> node) {
    if (Ast.hits(ast().range(ast().statementConditionKeyword(node)), position()))
      hitStatementConditionKeyword(node);
    else
      super.statementCondition(node);
  }

  @Override
  protected void statementLoopDo(Node<Statement.LoopDo> node) {
    if (Ast.hits(ast().range(ast().statementLoopDoKeywordDo(node)), position()))
      hitStatementLoopDoKeywordDo(node);
    else if (Ast.hits(ast().range(ast().statementLoopDoKeywordWhile(node)), position()))
      hitStatementLoopDoKeywordWhile(node);
    else
      super.statementLoopDo(node);
  }

  @Override
  protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
    if (Ast.hits(ast().range(ast().statementLoopWhileKeyword(node)), position()))
      hitStatementLoopWhileKeyword(node);
    else
      super.statementLoopWhile(node);
  }

  @Override
  protected void statementLoopFor(Node<Statement.LoopFor> node) {
    if (Ast.hits(ast().range(ast().statementLoopForKeyword(node)), position()))
      hitStatementLoopForKeyword(node);
    else
      super.statementLoopFor(node);
  }

  @Override
  protected void statementLoopForEach(Node<Statement.LoopForEach> node) {
    if (Ast.hits(ast().range(ast().statementLoopForEachKeyword(node)), position()))
      hitStatementLoopForEachKeyword(node);
    else if (Ast.hits(ast().range(ast().statementLoopForEachName(node)), position()))
      hitStatementLoopForEachName(node);
    else
      super.statementLoopForEach(node);
  }

  @Override
  protected void statementTryCatch(Node<Statement.Try.Catch> node) {
    if (Ast.hits(ast().range(ast().statementTryCatchName(node)), position()))
      hitStatementTryCatchName(node);
    else
      super.statementTryCatch(node);
  }

  @Override
  protected void expressionNull(Node<Expression.Null> node) {
    if (Ast.hits(ast().range(node), position()))
      hitExpressionNull(node);
    else
      super.expressionNull(node);
  }

  @Override
  protected void expressionBoolean(Node<Expression.Boolean> node) {
    if (Ast.hits(ast().range(node), position()))
      hitExpressionBoolean(node);
    else
      super.expressionBoolean(node);
  }

  @Override
  protected void expressionInteger(Node<Expression.Integer> node) {
    if (Ast.hits(ast().range(node), position()))
      hitExpressionInteger(node);
    else
      super.expressionInteger(node);
  }

  @Override
  protected void expressionDecimal(Node<Expression.Decimal> node) {
    if (Ast.hits(ast().range(node), position()))
      hitExpressionDecimal(node);
    else
      super.expressionDecimal(node);
  }

  @Override
  protected void expressionString(Node<Expression.String> node) {
    if (Ast.hits(ast().range(node), position()))
      hitExpressionString(node);
    else
      super.expressionString(node);
  }

  @Override
  protected void expressionSpecial(Node<Expression.Special> node) {
    if (Ast.hits(ast().range(node), position()))
      hitExpressionSpecial(node);
    else
      super.expressionSpecial(node);
  }

  @Override
  protected void expressionThis(Node<Expression.This> node) {
    if (Ast.hits(ast().range(node), position()))
      hitExpressionThis(node);
    else
      super.expressionThis(node);
  }

  @Override
  protected void expressionSuper(Node<Expression.Super> node) {
    if (Ast.hits(ast().range(node), position()))
      hitExpressionSuper(node);
    else
      super.expressionSuper(node);
  }

  @Override
  protected void expressionContainer(Node<Expression.Container> node) {
    if (Ast.hits(ast().range(ast().expressionContainerKeyword(node)), position()))
      hitExpressionContainerKeyword(node);
    else
      super.expressionContainer(node);
  }

  @Override
  protected void expressionAccess(Node<Expression.Access> node) {
    if (Ast.hits(ast().range(ast().expressionAccessName(node)), position()))
      hitExpressionAccessName(node);
    else
      super.expressionAccess(node);
  }

  @Override
  protected void expressionInvoke(Node<Expression.Invoke> node) {
    if (Ast.hits(ast().range(ast().expressionInvokeName(node)), position()))
      hitExpressionInvokeName(node);
    else
      super.expressionInvoke(node);
  }

  @Override
  protected void expressionObject(Node<Expression.Object> node) {
    if (Ast.hits(ast().range(ast().expressionObjectKeyword(node)), position()))
      hitExpressionObjectKeyword(node);
    else
      super.expressionObject(node);
  }

  @Override
  protected void expressionUnary(Node<Expression.Unary> node) {
    if (Ast.hits(ast().range(ast().expressionUnaryOperation(node)), position()))
      hitExpressionUnaryOperation(node);
    else
      super.expressionUnary(node);
  }

  @Override
  protected void expressionBinary(Node<Expression.Binary> node) {
    if (Ast.hits(ast().range(ast().expressionBinaryOperation(node)), position()))
      hitExpressionBinaryOperation(node);
    else
      super.expressionBinary(node);
  }

  @Override
  protected void expressionInstanceof(Node<Expression.Instanceof> node) {
    if (Ast.hits(ast().range(ast().expressionInstanceofKeyword(node)), position()))
      hitExpressionInstanceofKeyword(node);
    else
      super.expressionInstanceof(node);
  }

  @Override
  protected void definitionReference(Node<Definition.Reference> node) {
    if (Ast.hits(ast().range(ast().definitionReferenceName(node)), position()))
      hitDefinitionReferenceName(node);
    else
      super.definitionReference(node);
  }
}
