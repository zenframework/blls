package org.zenframework.z8.blls.ast;

public abstract class Visitor {
  protected abstract Ast ast();

  public void visit(int id) {
    switch (ast().data(id)) {
      case Source v -> source(new Node<>(id, v));
      case Import v -> import_(new Node<>(id, v));
      case Klass v -> klass(new Node<>(id, v));
      case Enumeration v -> enumeration(new Node<>(id, v));
      case Annotation v -> annotation(new Node<>(id, v));
      case Parameter v -> parameter(new Node<>(id, v));
      case Declaration v -> declaration(new Node<>(id, v));
      case Declaration.Child v -> {
        switch (v) {
          case Declaration.Field v2 -> declarationField(new Node<>(id, v2));
          case Declaration.Initialize v2 -> declarationInitialize(new Node<>(id, v2));
          case Declaration.Method v2 -> declarationMethod(new Node<>(id, v2));
          case Declaration.Priority v2 -> declarationPriority(new Node<>(id, v2));
          case Declaration.Cast v2 -> declarationCast(new Node<>(id, v2));
          case Declaration.Operator v2 -> declarationOperator(new Node<>(id, v2));
          case Declaration.Records v2 -> declarationRecords(new Node<>(id, v2));
        }
      }
      case Declaration.Records.Child v -> declarationRecordsChild(new Node<>(id, v));
      case Enumeration.Child v -> enumerationChild(new Node<>(id, v));
      case Statement v -> {
        switch (v) {
          case Statement.Block v2 -> statementBlock(new Node<>(id, v2));
          case Statement.Expression v2 -> statementExpression(new Node<>(id, v2));
          case Statement.Variable v2 -> statementVariable(new Node<>(id, v2));
          case Statement.Return v2 -> statementReturn(new Node<>(id, v2));
          case Statement.Break v2 -> statementBreak(new Node<>(id, v2));
          case Statement.Continue v2 -> statementContinue(new Node<>(id, v2));
          case Statement.Throw v2 -> statementThrow(new Node<>(id, v2));
          case Statement.Try v2 -> statementTry(new Node<>(id, v2));
          case Statement.Condition v2 -> statementCondition(new Node<>(id, v2));
          case Statement.LoopDo v2 -> statementLoopDo(new Node<>(id, v2));
          case Statement.LoopWhile v2 -> statementLoopWhile(new Node<>(id, v2));
          case Statement.LoopFor v2 -> statementLoopFor(new Node<>(id, v2));
          case Statement.LoopForEach v2 -> statementLoopForEach(new Node<>(id, v2));
        }
      }
      case Statement.Try.Catch v -> statementTryCatch(new Node<>(id, v));
      case Expression v -> {
        switch (v) {
          case Expression.Paren v2 -> expressionParen(new Node<>(id, v2));
          case Expression.Null v2 -> expressionNull(new Node<>(id, v2));
          case Expression.Boolean v2 -> expressionBoolean(new Node<>(id, v2));
          case Expression.Integer v2 -> expressionInteger(new Node<>(id, v2));
          case Expression.Decimal v2 -> expressionDecimal(new Node<>(id, v2));
          case Expression.String v2 -> expressionString(new Node<>(id, v2));
          case Expression.Special v2 -> expressionSpecial(new Node<>(id, v2));
          case Expression.This v2 -> expressionThis(new Node<>(id, v2));
          case Expression.Super v2 -> expressionSuper(new Node<>(id, v2));
          case Expression.Container v2 -> expressionContainer(new Node<>(id, v2));
          case Expression.New v2 -> expressionNew(new Node<>(id, v2));
          case Expression.Array v2 -> expressionArray(new Node<>(id, v2));
          case Expression.Access v2 -> expressionAccess(new Node<>(id, v2));
          case Expression.Index v2 -> expressionIndex(new Node<>(id, v2));
          case Expression.Invoke v2 -> expressionInvoke(new Node<>(id, v2));
          case Expression.Cast v2 -> expressionCast(new Node<>(id, v2));
          case Expression.Object v2 -> expressionObject(new Node<>(id, v2));
          case Expression.Unary v2 -> expressionUnary(new Node<>(id, v2));
          case Expression.Binary v2 -> expressionBinary(new Node<>(id, v2));
          case Expression.Instanceof v2 -> expressionInstanceof(new Node<>(id, v2));
          case Expression.Ternary v2 -> expressionTernary(new Node<>(id, v2));
        }
      }
      case Expression.Array.Child.Value v -> expressionArrayChildValue(new Node<>(id, v));
      case Expression.Array.Child.Entry v -> expressionArrayChildEntry(new Node<>(id, v));
      case Expression.Invoke.Argument v -> expressionInvokeArgument(new Node<>(id, v));
      case Definition v -> {
        switch (v) {
          case Definition.Reference v2 -> definitionReference(new Node<>(id, v2));
          case Definition.Array v2 -> definitionArray(new Node<>(id, v2));
          case Definition.Map v2 -> definitionMap(new Node<>(id, v2));
        }
      }
    }
  }

  protected void source(Node<Source> node) {
    ast().siblings(node.data().importsId()).forEach(this::visit);
    ast().siblings(node.data().childrenId()).forEach(this::visit);
  }

  protected void import_(Node<Import> node) {
    visit(node.data().valueId());
  }

  protected void klass(Node<Klass> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    if (node.data().extendedId() > 0)
      visit(node.data().extendedId());
    visit(node.data().childId());
  }

  protected void enumeration(Node<Enumeration> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    ast().siblings(node.data().childrenId()).forEach(this::visit);
  }

  protected void annotation(Node<Annotation> node) {
    if (node.data().valueId() > 0)
      visit(node.data().valueId());
  }

  protected void parameter(Node<Parameter> node) {
    visit(node.data().definitionId());
  }

  protected void declaration(Node<Declaration> node) {
    ast().siblings(node.data().childrenId()).forEach(this::visit);
  }

  protected void declarationField(Node<Declaration.Field> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().definitionId());
    if (node.data().valueId() > 0)
      visit(node.data().valueId());
  }

  protected void declarationInitialize(Node<Declaration.Initialize> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().childId());
  }

  protected void declarationMethod(Node<Declaration.Method> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().definitionId());
    ast().siblings(node.data().parametersId()).forEach(this::visit);
    if (node.data().blockId() > 0)
      visit(node.data().blockId());
  }

  protected void declarationPriority(Node<Declaration.Priority> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    ast().siblings(node.data().parametersId()).forEach(this::visit);
    if (node.data().blockId() > 0)
      visit(node.data().blockId());
  }

  protected void declarationCast(Node<Declaration.Cast> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().definitionId());
    ast().siblings(node.data().parametersId()).forEach(this::visit);
    if (node.data().blockId() > 0)
      visit(node.data().blockId());
  }

  protected void declarationOperator(Node<Declaration.Operator> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().definitionId());
    ast().siblings(node.data().parametersId()).forEach(this::visit);
    if (node.data().blockId() > 0)
      visit(node.data().blockId());
  }

  protected void declarationRecords(Node<Declaration.Records> node) {
    ast().siblings(node.data().childrenId()).forEach(this::visit);
  }

  protected void declarationRecordsChild(Node<Declaration.Records.Child> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().valueId());
  }

  protected void enumerationChild(Node<Enumeration.Child> node) {
  }

  protected void statementBlock(Node<Statement.Block> node) {
    ast().siblings(node.data().statementsId()).forEach(this::visit);
  }

  protected void statementExpression(Node<Statement.Expression> node) {
    visit(node.data().valueId());
  }

  protected void statementVariable(Node<Statement.Variable> node) {
    visit(node.data().definitionId());
    if (node.data().valueId() > 0)
      visit(node.data().valueId());
  }

  protected void statementReturn(Node<Statement.Return> node) {
    if (node.data().valueId() > 0)
      visit(node.data().valueId());
  }

  protected void statementBreak(Node<Statement.Break> node) {
  }

  protected void statementContinue(Node<Statement.Continue> node) {
  }

  protected void statementThrow(Node<Statement.Throw> node) {
    visit(node.data().valueId());
  }

  protected void statementTry(Node<Statement.Try> node) {
    visit(node.data().blockId());
    ast().siblings(node.data().catchesId()).forEach(this::visit);
    if (node.data().blockFinallyId() > 0)
      visit(node.data().blockFinallyId());
  }

  protected void statementCondition(Node<Statement.Condition> node) {
    visit(node.data().conditionId());
    visit(node.data().thenId());
    if (node.data().otherId() > 0)
      visit(node.data().otherId());
  }

  protected void statementLoopDo(Node<Statement.LoopDo> node) {
    visit(node.data().statementId());
    visit(node.data().conditionId());
  }

  protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
    visit(node.data().conditionId());
    visit(node.data().statementId());
  }

  protected void statementLoopFor(Node<Statement.LoopFor> node) {
    if (node.data().initializerId() > 0)
      visit(node.data().initializerId());
    visit(node.data().conditionId());
    visit(node.data().updateId());
    visit(node.data().statementId());
  }

  protected void statementLoopForEach(Node<Statement.LoopForEach> node) {
    visit(node.data().definitionId());
    visit(node.data().expressionId());
    visit(node.data().statementId());
  }

  protected void statementTryCatch(Node<Statement.Try.Catch> node) {
    visit(node.data().definitionId());
    visit(node.data().blockId());
  }

  protected void expressionParen(Node<Expression.Paren> node) {
    visit(node.data().childId());
  }

  protected void expressionNull(Node<Expression.Null> node) {
  }

  protected void expressionBoolean(Node<Expression.Boolean> node) {
  }

  protected void expressionInteger(Node<Expression.Integer> node) {
  }

  protected void expressionDecimal(Node<Expression.Decimal> node) {
  }

  protected void expressionString(Node<Expression.String> node) {
  }

  protected void expressionSpecial(Node<Expression.Special> node) {
  }

  protected void expressionThis(Node<Expression.This> node) {
  }

  protected void expressionSuper(Node<Expression.Super> node) {
  }

  protected void expressionContainer(Node<Expression.Container> node) {
    if (node.data().childId() > 0)
      visit(node.data().childId());
  }

  protected void expressionNew(Node<Expression.New> node) {
    visit(node.data().definitionId());
  }

  protected void expressionArray(Node<Expression.Array> node) {
    if (node.data().definitionId() > 0)
      visit(node.data().definitionId());
    ast().siblings(node.data().childrenId()).forEach(this::visit);
  }

  protected void expressionAccess(Node<Expression.Access> node) {
    if (node.data().childId() > 0)
      visit(node.data().childId());
  }

  protected void expressionIndex(Node<Expression.Index> node) {
    visit(node.data().childId());
    visit(node.data().valueId());
  }

  protected void expressionInvoke(Node<Expression.Invoke> node) {
    if (node.data().childId() > 0)
      visit(node.data().childId());
    ast().siblings(node.data().argumentsId()).forEach(this::visit);
  }

  protected void expressionCast(Node<Expression.Cast> node) {
    visit(node.data().definitionId());
    visit(node.data().valueId());
  }

  protected void expressionObject(Node<Expression.Object> node) {
    visit(node.data().childId());
  }

  protected void expressionUnary(Node<Expression.Unary> node) {
    visit(node.data().valueId());
  }

  protected void expressionBinary(Node<Expression.Binary> node) {
    visit(node.data().leftId());
    visit(node.data().rightId());
  }

  protected void expressionInstanceof(Node<Expression.Instanceof> node) {
    visit(node.data().valueId());
    visit(node.data().definitionId());
  }

  protected void expressionTernary(Node<Expression.Ternary> node) {
    visit(node.data().conditionId());
    visit(node.data().thenId());
    visit(node.data().otherId());
  }

  protected void expressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
    visit(node.data().valueId());
  }

  protected void expressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
    visit(node.data().keyId());
    visit(node.data().valueId());
  }

  protected void expressionInvokeArgument(Node<Expression.Invoke.Argument> node) {
    visit(node.data().valueId());
  }

  protected void definitionReference(Node<Definition.Reference> node) {
    if (node.data().childId() > 0)
      visit(node.data().childId());
  }

  protected void definitionArray(Node<Definition.Array> node) {
    visit(node.data().valueId());
  }

  protected void definitionMap(Node<Definition.Map> node) {
    visit(node.data().valueId());
    visit(node.data().keyId());
  }
}
