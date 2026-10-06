package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/// Шаг 2:
/// - Разрешить Declaration.Field
/// - Разрешить Declaration.Initialize
/// - Разрешить Statement.Variable
/// - Разрешить Statement.Return
/// - Разрешить Statement.Throw
/// - Разрешить Statement.Try.Catch
/// - Разрешить Statement.Condition
/// - Разрешить Statement.LoopWhile
/// - Разрешить Statement.LoopDo
/// - Разрешить Statement.LoopFor
/// - Разрешить Expression.Paren
/// - Разрешить Expression.This
/// - Разрешить Expression.Super
/// - Разрешить Expression.Container
/// - Разрешить Expression.Identifier
/// - Разрешить Expression.Array
/// - Разрешить Expression.Array.Child.Value
/// - Разрешить Expression.Array.Child.Entry
/// - Разрешить Expression.Access
/// - Разрешить Expression.Index
/// - Разрешить Expression.Invoke
/// - Разрешить Expression.Cast
/// - Разрешить Expression.Object
/// - Разрешить Expression.Unary
/// - Разрешить Expression.Binary
/// - Разрешить Expression.Instanceof
/// - Разрешить Expression.Ternary
public abstract class Visitor2 extends Visitor {
  // Передаётся как подсказка, к какому типу лучше всего подогнать
  // Узел может забить на эту подсказку
  private Expression.@Nullable Type hint = null;
  private Expression.@Nullable Type hintOld = null;

  @Override
  protected abstract Ast ast();

  protected abstract Optional<Expression.Type.Declaration> primaryOf(String name);

  protected abstract void typeSet(Node<? extends Expression> node, Expression.Type value);

  /// `source`: не содержит оператор `operation`
  protected abstract void diagnosticAbsent(Node<?> node, Expression.Type source, Operation operation);

  /// Не удалось выбрать подходящий метод
  protected abstract void diagnosticAmbiguous(Node<?> node, Stream<? extends Node<? extends Declaration.Child>> candidates);

  /// Неправильный тип: `type`
  protected abstract void diagnosticWrong(Node<?> node, Expression.Type type);

  /// Не применимо в статическом контексте
  protected abstract void diagnosticInstance(Node<?> node);

  /// Ожидается `target`: дано `source`
  protected abstract void diagnosticType(Node<?> node, Expression.Type target, Expression.Type source);

  /// Невозможно выявить тип
  protected abstract void diagnosticTypeNoHint(Node<?> node);

  /// Метод не возвращает значения
  protected abstract void diagnosticStatementReturnNothing(Node<?> node);

  /// Метод должен вернуть `target`
  protected abstract void diagnosticStatementReturnSomething(Node<?> node, Expression.Type target);

  /// Ожидается дата, временной отрезок, байты или GUID
  protected abstract void diagnosticExpressionSpecial(Node<Expression.Special> node);

  /// Нет наследования
  protected abstract void diagnosticExpressionSuper(Node<Expression.Super> node);

  /// Нет вложения
  protected abstract void diagnosticExpressionContainer(Node<Expression.Container> node);

  /// Ожидается массив или словарь
  protected abstract void diagnosticExpressionArray(Node<Expression.Array> node);

  /// Ожидается пара ключ-значение
  protected abstract void diagnosticExpressionArrayChildValue(Node<Expression.Array.Child.Value> node);

  /// Ожидается элемент массива
  protected abstract void diagnosticExpressionArrayChildEntry(Node<Expression.Array.Child.Entry> node);

  /// `type`: не содержит `node`.name
  protected abstract void diagnosticExpressionAccessAbsent(Node<Expression.Access> node, Expression.Type type);

  /// Не удалось разрешить
  protected abstract void diagnosticExpressionAccessFailed(Node<Expression.Access> node);

  /// `pack`: не содержит `node`.name
  protected abstract void diagnosticExpressionAccessStaticAbsent(Node<Expression.Access> node, Ast.Pack pack);

  /// `declaration`: не содержит `node`.name
  protected abstract void diagnosticExpressionAccessStaticAbsentDeclaration(Node<Expression.Access> node, Node<Declaration> declaration);

  /// `enumeration`: не содержит `node`.name
  protected abstract void diagnosticExpressionAccessStaticAbsentEnumeration(Node<Expression.Access> node, Node<Enumeration> enumeration);

  /// Не удалось разрешить
  protected abstract void diagnosticExpressionAccessStaticLack(Node<Expression.Access> node, Ast.PackChild result);

  /// Ожидается массив или словарь
  protected abstract void diagnosticExpressionIndex(Node<Expression.Index> node);

  /// `type`: не содержит `node`.name
  protected abstract void diagnosticExpressionInvokeAbsent(Node<Expression.Invoke> node, Expression.Type type);

  /// Не удалось выбрать подходящий метод
  protected abstract void diagnosticExpressionInvokeAmbiguousSynthetic(Node<Expression.Invoke> node, Stream<Synthetic.Method> candidates);

  /// `declaration`: не содержит `node`.name
  protected abstract void diagnosticExpressionInvokeStaticAbsentDeclaration(Node<Expression.Invoke> node, Node<Declaration> declaration);

  /// `enumeration`: не содержит `node`.name
  protected abstract void diagnosticExpressionInvokeStaticAbsentEnumeration(Node<Expression.Invoke> node, Node<Enumeration> enumeration);

  /// Выражение всегда выполняется: `source` равен или наследует `target`
  protected abstract void diagnosticExpressionCastSuccess(Node<Expression.Cast> node, Expression.Type source, Expression.Type target);

  /// Если значение != null: выражение невыполнимо: `source` нельзя привести к `target`
  protected abstract void diagnosticExpressionCastFail(Node<Expression.Cast> node, Expression.Type source, Expression.Type target);

  /// Если значение != null: выражение истинно
  protected abstract void diagnosticExpressionInstanceofSuccess(Node<Expression.Instanceof> node);

  /// Выражение ложно: `target` не наследует `source`
  protected abstract void diagnosticExpressionInstanceofFail(Node<Expression.Instanceof> node, Expression.Type source, Expression.Type target);

  private boolean isStatic(Node<? extends Expression> node) {
    int enclosingId = ast().parents(node.id())
      .filter(it -> switch (ast().data(it)) {
        case Annotation _,
             Declaration.Field _,
             Declaration.Initialize _,
             Declaration.Method _,
             Declaration.Priority _,
             Declaration.Cast _,
             Declaration.Operator _ -> true;
        default -> false;
      })
      .findFirst()
      .orElseThrow();
    return switch (ast().data(enclosingId)) {
      case Annotation _ -> switch (ast().parent(enclosingId).data()) {
        case Klass _, Enumeration _ -> true;
        case Declaration.Field v -> (Declaration.STATIC & v.modifiers()) != 0;
        case Declaration.Initialize _ -> false;
        case Declaration.Method v -> (Declaration.STATIC & v.modifiers()) != 0;
        case Declaration.Priority v -> (Declaration.STATIC & v.modifiers()) != 0;
        case Declaration.Cast v -> (Declaration.STATIC & v.modifiers()) != 0;
        case Declaration.Operator v -> (Declaration.STATIC & v.modifiers()) != 0;
        case Declaration.Records.Child _ -> true;
        default -> throw new AssertionError();
      };
      case Declaration.Field v -> (Declaration.STATIC & v.modifiers()) != 0;
      case Declaration.Initialize _ -> false;
      case Declaration.Method v -> (Declaration.STATIC & v.modifiers()) != 0;
      case Declaration.Priority v -> (Declaration.STATIC & v.modifiers()) != 0;
      case Declaration.Cast v -> (Declaration.STATIC & v.modifiers()) != 0;
      case Declaration.Operator v -> (Declaration.STATIC & v.modifiers()) != 0;
      default -> throw new AssertionError();
    };
  }

  private void visitHinted(int id, Expression.@Nullable Type hint) {
    this.hintOld = hint;
    visit(id);
  }

  @Override
  public void visit(int id) {
    // При вызове visitHinted() подсказка будет доступна только в течение одного visit()
    hint = hintOld;
    hintOld = null;

    super.visit(id);
  }

  @Override
  protected void declarationField(Node<Declaration.Field> node) {
    ast().siblings(node.data().annotationsId()).forEach(this::visit);

    var typeTarget = ast().typeOf(node.data().definition(ast())).orElse(null);

    if (node.data().valueId() > 0)
      visitHinted(node.data().valueId(), typeTarget);

    var typeSource = node.data().value(ast())
      .flatMap(it -> ast().type(it.data().value(ast())))
      .orElse(null);

    if (typeTarget != null && typeSource != null) new Accessor.Operator.Assign() {
      @Override
      protected Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      public Expression.Type type() {
        return typeSource;
      }

      @Override
      public void setCast(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.DeclarationField.class, e -> e.valueCastId = node2.id());
      }

      @Override
      protected Expression.Type target() {
        return typeTarget;
      }

      @Override
      protected void ambiguous(Stream<? extends Node<Declaration.Operator>> result) {
        diagnosticAmbiguous(node, result);
      }

      @Override
      protected void success(Node<Declaration.Operator> result) {
        ast().extraUpdate(node, Extra.DeclarationField.class, e -> e.resolvedAssignId = result.id());
      }

      @Override
      protected void failedAssign() {
        diagnosticType(node, target(), type());
      }

      @Override
      protected void success() {
      }
    }.run(typeTarget);
  }

  @Override
  protected void declarationInitialize(Node<Declaration.Initialize> node) {
    var declaration = ast().declarationOf(node);

    int accessId = ast().initializeAccesses(node.data())
      .reduce((_, b) -> b)
      .orElseThrow()
      .id();
    Expression.Type typeSource = new Expression.Type.Declaration(declaration.id());

    var parents = ast().parents(accessId).iterator();
    accessId = parents.next();

    while (ast().data(accessId) instanceof Expression.Access v && typeSource != null) {
      var n = new Node<>(accessId, v);
      var source = typeSource;

      // в Declaration.Initializer алгоритм разрешения отличается, т.к. можно обращаться только к полям
      var afor = new Accessor.FieldOrRecord() {
        Expression.@Nullable Type result = null;

        @Override
        protected Ast ast() {
          return Visitor2.this.ast();
        }

        @Override
        protected Node<Declaration> declaration() {
          return declaration;
        }

        @Override
        protected void absent() {
          diagnosticExpressionAccessAbsent(n, source);
        }

        @Override
        protected int nameId() {
          return v.nameId();
        }

        @Override
        protected void success(Proxy result) {
          switch (result) {
            case ProxyField v -> {
              ast().extraUpdate(n, Extra.ExpressionAccess.class, e -> e.setResolvedDeclarationField(v.node()));

              this.result = ast().typeOf(v.node().data().definition(ast())).orElse(null);
              if (this.result != null) {
                typeSet(n, this.result);
              }
            }
            case ProxyRecordsChild _ -> absent();
          }
        }
      };
      afor.run(source);

      accessId = parents.next();
      typeSource = afor.result;
    }

    if (node.data().child(ast()).data().value(ast()) instanceof Node(int id, Expression.Binary v)) {
      expressionBinary(new Node<>(id, v), typeSource);
    }
  }

  @Override
  protected void statementExpression(Node<Statement.Expression> node) {
    visitHinted(node.data().valueId(), hint);
  }

  @Override
  protected void statementVariable(Node<Statement.Variable> node) {
    if (node.data().valueId() == 0)
      return;

    var typeTarget = ast().typeOf(node.data().definition(ast())).orElse(null);

    visitHinted(node.data().valueId(), typeTarget);

    var typeSource = ast().type(node.data().value(ast()).orElseThrow()).orElse(null);
    if (typeTarget != null && typeSource != null) new Accessor.Operator.Assign() {
      @Override
      protected Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      public Expression.Type type() {
        return typeSource;
      }

      @Override
      public void setCast(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.StatementVariable.class, e -> e.valueCastId = node2.id());
      }

      @Override
      protected Expression.Type target() {
        return typeTarget;
      }

      @Override
      protected void ambiguous(Stream<? extends Node<Declaration.Operator>> result) {
        diagnosticAmbiguous(node, result);
      }

      @Override
      protected void success(Node<Declaration.Operator> result) {
        ast().extraUpdate(node, Extra.StatementVariable.class, e -> e.resolvedAssignId = result.id());
      }

      @Override
      protected void failedAssign() {
        diagnosticType(node, target(), type());
      }

      @Override
      protected void success() {
      }
    }.run(typeTarget);
  }

  @Override
  protected void statementReturn(Node<Statement.Return> node) {
    var declarationChild = ast().parents(node.id())
      .mapToObj(ast()::node)
      .gather(Ast.only(Declaration.Child.class))
      .findFirst()
      .orElseThrow();

    var typeTarget = switch (declarationChild.data()) {
      case Declaration.Method v -> ast().typeOf(v.definition(ast())).orElse(null);
      // вернуть тот же тип, в котором объявлен
      case Declaration.Priority _ -> new Expression.Type.Declaration(ast().declarationOf(node).id());
      case Declaration.Cast v -> ast().typeOf(v.definition(ast())).orElse(null);
      case Declaration.Operator v -> ast().typeOf(v.definition(ast())).orElse(null);
      default -> throw new AssertionError();
    };

    if (node.data().valueId() > 0) {
      visitHinted(node.data().valueId(), typeTarget);
    }

    var typeVoid = primaryOf("void").orElse(null);
    if (typeTarget == null || typeVoid == null) return;

    if (typeTarget.equals(typeVoid)) {
      if (node.data().valueId() > 0) {
        diagnosticStatementReturnNothing(node);
      }
    } else if (node.data().valueId() > 0) {
      var typeSource = ast().type(node.data().value(ast()).orElseThrow()).orElse(null);
      if (typeSource == null) return;

      new CasterEnhanced() {
        @Override
        protected Ast ast() {
          return Visitor2.this.ast();
        }

        @Override
        protected void failed() {
          diagnosticType(node.data().value(ast()).orElseThrow(), typeTarget, typeSource);
        }

        @Override
        protected void success(int distance) {
          resolvePriority();
        }

        @Override
        protected void success() {
          resolvePriority();
        }

        @Override
        protected Node<Declaration> declaration() {
          return ast().declarationOf(node);
        }

        @Override
        protected void success(Node<Declaration.Cast> node2) {
          ast().extraUpdate(node, Extra.StatementReturn.class, e -> e.valueCastId = node2.id());
          resolvePriority();
        }

        void resolvePriority() {
          if (typeTarget instanceof Expression.Type.Declaration) new Accessor.Priority() {
            @Override
            protected Ast ast() {
              return Visitor2.this.ast();
            }

            @Override
            protected Node<Declaration> declaration() {
              return ast().declarationOf(node);
            }

            @Override
            protected void absent() {
            }

            @Override
            protected void success(Node<Declaration.Priority> result) {
              ast().extraUpdate(node, Extra.StatementReturn.class, e -> e.resolvedPriorityId = result.id());
            }
          }.run(typeTarget);
        }
      }.run(typeSource, typeTarget);
    } else {
      diagnosticStatementReturnSomething(node, typeTarget);
    }
  }

  @Override
  protected void statementThrow(Node<Statement.Throw> node) {
    var typeTarget = primaryOf("exception").orElse(null);

    new VisitAndCast(node.data().value(ast()), typeTarget) {
      @Override
      protected void success(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.StatementThrow.class, e -> e.valueCastId = node2.id());
      }
    }.run();
  }

  @Override
  protected void statementCondition(Node<Statement.Condition> node) {
    var typeBool = primaryOf("bool").orElse(null);

    new VisitAndCast(node.data().condition(ast()), typeBool) {
      @Override
      protected void success(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.StatementCondition.class, e -> e.conditionCastId = node2.id());
      }
    }.run();

    visit(node.data().thenId());
    if (node.data().otherId() > 0)
      visit(node.data().otherId());
  }

  @Override
  protected void statementLoopDo(Node<Statement.LoopDo> node) {
    var typeBool = primaryOf("bool").orElse(null);

    visit(node.data().statementId());

    new VisitAndCast(node.data().condition(ast()), typeBool) {
      @Override
      protected void success(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.StatementLoopDo.class, e -> e.conditionCastId = node2.id());
      }
    }.run();
  }

  @Override
  protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
    var typeBool = primaryOf("bool").orElse(null);

    new VisitAndCast(node.data().condition(ast()), typeBool) {
      @Override
      protected void success(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.StatementLoopWhile.class, e -> e.conditionCastId = node2.id());
      }
    }.run();

    visit(node.data().statementId());
  }

  @Override
  protected void statementLoopFor(Node<Statement.LoopFor> node) {
    if (node.data().initializerId() > 0)
      visit(node.data().initializerId());

    var typeBool = primaryOf("bool").orElse(null);
    new VisitAndCast(node.data().condition(ast()), typeBool) {
      @Override
      protected void success(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.StatementLoopFor.class, e -> e.conditionCastId = node2.id());
      }
    }.run();

    visit(node.data().updateId());
    visit(node.data().statementId());
  }

  @Override
  protected void statementLoopForEach(Node<Statement.LoopForEach> node) {
    visit(node.data().expressionId());
    visit(node.data().statementId());

    var typeSource = ast().type(node.data().expression(ast())).orElse(null);
    if (typeSource == null) return;

    if (!(typeSource instanceof Expression.Type.Array(var typeSourceChild))) {
      diagnosticWrong(node.data().expression(ast()), typeSource);
      return;
    }

    var typeTarget = ast().typeOf(node.data().definition(ast())).orElse(null);
    if (typeTarget == null) return;

    if (!Caster.runSimple(ast(), typeSourceChild, typeTarget)) {
      diagnosticType(node.data().definition(ast()), typeSourceChild, typeTarget);
    }
  }

  @Override
  protected void statementTryCatch(Node<Statement.Try.Catch> node) {
    visit(node.data().blockId());

    var typeTarget = primaryOf("exception").orElse(null);
    var typeSource = ast().typeOf(node.data().definition(ast())).orElse(null);
    if (typeTarget == null || typeSource == null) return;

    if (!Caster.runSimple(ast(), typeSource, typeTarget)) {
      diagnosticType(node.data().definition(ast()), typeTarget, typeSource);
    }
  }

  @Override
  protected void expressionParen(Node<Expression.Paren> node) {
    visitHinted(node.data().childId(), hint);

    var typeChild = ast().type(node.data().child(ast())).orElse(null);
    if (typeChild == null) return;

    if (!(node.data().child(ast()).data() instanceof Expression.Ternary) && typeChild instanceof Expression.Type.Declaration) {
      new Accessor.Priority() {
        @Override
        protected Ast ast() {
          return Visitor2.this.ast();
        }

        @Override
        protected Node<Declaration> declaration() {
          return ast().declarationOf(node);
        }

        @Override
        protected void absent() {
        }

        @Override
        protected void success(Node<Declaration.Priority> result) {
          ast().extraUpdate(node, Extra.ExpressionParen.class, e -> e.resolvedPriorityId = result.id());
        }
      }.run(typeChild);
    }

    typeSet(node, typeChild);
  }

  @Override
  protected void expressionNull(Node<Expression.Null> node) {
    typeSet(node, new Expression.Type.Null());
  }

  @Override
  protected void expressionBoolean(Node<Expression.Boolean> node) {
    primaryOf("bool").ifPresent(it -> typeSet(node, it));
  }

  @Override
  protected void expressionInteger(Node<Expression.Integer> node) {
    primaryOf("int").ifPresent(it -> typeSet(node, it));
  }

  @Override
  protected void expressionDecimal(Node<Expression.Decimal> node) {
    primaryOf("decimal").ifPresent(it -> typeSet(node, it));
  }

  @Override
  protected void expressionString(Node<Expression.String> node) {
    primaryOf("string").ifPresent(it -> typeSet(node, it));
  }

  @Override
  protected void expressionSpecial(Node<Expression.Special> node) {
    String v = node.data().value(ast());

    String primaryName;
    if (Expression.Special.REGEX_DATE.matcher(v).matches()) {
      primaryName = "date";
    } else if (Expression.Special.REGEX_DATESPAN.matcher(v).matches()) {
      primaryName = "datespan";
    } else if (Expression.Special.REGEX_BINARY.matcher(v).matches()) {
      primaryName = "binary";
    } else if (Expression.Special.REGEX_GUID.matcher(v).matches()) {
      primaryName = "guid";
    } else {
      diagnosticExpressionSpecial(node);
      return;
    }

    primaryOf(primaryName).ifPresent(it -> typeSet(node, it));
  }

  @Override
  protected void expressionThis(Node<Expression.This> node) {
    typeSet(node, new Expression.Type.Declaration(ast().declarationOf(node).id()));

    if (isStatic(node)) {
      // Нельзя использовать this в статическом контексте
      diagnosticInstance(node);
    }
  }

  @Override
  protected void expressionSuper(Node<Expression.Super> node) {
    if (isStatic(node)) {
      // Нельзя использовать super в статическом контексте
      diagnosticInstance(node);
    }

    var declaration = ast().inheritance(ast().declarationOf(node))
      // При проходе по наследованию объявлений пропускаем текущее
      .skip(1)
      .findFirst()
      .orElse(null);
    if (declaration == null) {
      // Нельзя использовать super в объявлении без родительского класса
      diagnosticExpressionSuper(node);
    } else {
      typeSet(node, new Expression.Type.Declaration(declaration.id()));
    }
  }

  @Override
  protected void expressionContainer(Node<Expression.Container> node) {
    super.expressionContainer(node);

    if (isStatic(node)) {
      // Нельзя использовать container в статическом контексте
      diagnosticInstance(node);
    }

    var declaration = node.data().child(ast())
      // container обязательно разрешится в Expression.Type.Declaration
      .map(it -> ((Expression.Type.Declaration) ast().type(it).orElseThrow()).node(ast()))
      .orElseGet(() -> ast().declarationOf(node));

    declaration = ast().nesting(declaration)
      // При проходе по вложенным объявлениям пропускаем текущее
      .skip(1)
      .findFirst()
      .orElse(null);
    if (declaration == null) {
      // Есть текущее объявление не вложено - container использовать нельзя
      diagnosticExpressionContainer(node);
    } else {
      typeSet(node, new Expression.Type.Declaration(declaration.id()));
    }
  }

  @Override
  protected void expressionNew(Node<Expression.New> node) {
    ast().typeOf(node.data().definition(ast())).ifPresent(it -> typeSet(node, it));
  }

  @Override
  protected void expressionArray(Node<Expression.Array> node) {
    Expression.Type typeTarget = null;
    if (node.data().definitionId() > 0) {
      typeTarget = ast().typeOf(node.data().definition(ast()).orElseThrow()).orElse(null);
    } else if (hint != null) {
      typeTarget = hint;
    } else {
      diagnosticTypeNoHint(node);
    }

    switch (typeTarget) {
      case null -> {
        if (node.data().childrenId() > 0)
          visit(node.data().childrenId());
      }
      case Expression.Type.Array v -> {
        node.data().children(ast())
          .<Node<Expression.Array.Child.Value>>mapMulti((it, downstream) -> {
            switch (it.data()) {
              case Expression.Array.Child.Value v2 -> downstream.accept(new Node<>(it.id(), v2));
              case Expression.Array.Child.Entry v2 -> {
                visit(it.id());
                diagnosticExpressionArrayChildEntry(new Node<>(it.id(), v2));
              }
            }
          }).forEach(it -> visitHinted(it.id(), v));

        typeSet(node, typeTarget);
      }
      case Expression.Type.Map v -> {
        node.data().children(ast())
          .<Node<Expression.Array.Child.Entry>>mapMulti((it, downstream) -> {
            switch (it.data()) {
              case Expression.Array.Child.Value v2 -> {
                visit(it.id());
                diagnosticExpressionArrayChildValue(new Node<>(it.id(), v2));
              }
              case Expression.Array.Child.Entry v2 -> downstream.accept(new Node<>(it.id(), v2));
            }
          }).forEach(it -> visitHinted(it.id(), v));

        typeSet(node, typeTarget);
      }
      default -> diagnosticExpressionArray(node);
    }
  }

  @Override
  protected void expressionAccess(Node<Expression.Access> node) {
    super.expressionAccess(node);

    abstract class Impl extends Accessor.FieldOrRecord {
      @Override
      protected final int nameId() {
        return node.data().nameId();
      }

      @Override
      protected final void success(Accessor.FieldOrRecord.Proxy result) {
        switch (result) {
          case Accessor.FieldOrRecord.ProxyField v -> {
            ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.setResolvedDeclarationField(v.node()));

            ast().typeOf(v.node().data().definition(ast())).ifPresent(it -> typeSet(node, it));
          }
          case Accessor.FieldOrRecord.ProxyRecordsChild v -> {
            ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.setResolvedDeclarationRecordsChild(v.node()));

            primaryOf("guid").ifPresent(it -> typeSet(node, it));
          }
        }
      }

      @Override
      protected final Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected final Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      Stream<Proxy> searchStatic(Node<Declaration> node2) {
        return node2.data().children(ast())
          .gather(Ast.only(Declaration.Field.class))
          .filter(it -> it.data().nameId() == nameId())
          .<Proxy>map(ProxyField::new)
          .filter(it -> (Declaration.STATIC & modifiers(it)) != 0)
          .findFirst()
          .or(() ->
            node2.data().children(ast())
              .gather(Ast.only(Declaration.Records.class))
              .flatMap(it -> it.data().children(ast()))
              .filter(it -> it.data().nameId() == nameId())
              .map(ProxyRecordsChild::new)
              .findFirst()
          ).stream();
      }

      void runStatic(Node<Declaration> declaration) {
        // little hack
        // skip Protector::run because it's object-context, we are in static context
        // rest of logic is the same
        success(Stream.concat(
          searchStatic(declaration)
            .filter(it -> Declaration.PRIVATE >= protection(it)),
          ast().inheritance(declaration)
            .skip(1)
            .flatMap(this::searchStatic)
            .filter(it -> Declaration.PROTECTED >= protection(it))
        ));
      }
    }

    if (node.data().childId() == 0) {
      // доступ к переменной?
      new Variabler() {
        @Override
        protected Ast ast() {
          return Visitor2.this.ast();
        }

        @Override
        protected void absent() {
          // доступ к полю?
          var impl = new Impl() {
            @Override
            protected void absent() {
              // доступ к пакету/типу?
              new ResolverUnqualified() {
                @Override
                protected Ast ast() {
                  return Visitor2.this.ast();
                }

                @Override
                protected Optional<Expression.Type.Declaration> primaryOf(String name) {
                  return Visitor2.this.primaryOf(name);
                }

                @Override
                protected Node<?> node() {
                  return node;
                }

                @Override
                protected String name() {
                  return node.data().name(ast());
                }

                @Override
                protected void absent() {
                  diagnosticExpressionAccessFailed(node);
                }

                @Override
                protected void success(Ast.PackChild result) {
                  ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.setResolved(result));

                  if (result instanceof Ast.Pack) {
                    if (!(ast().parent(node.id()).data() instanceof Expression.Access)) {
                      diagnosticExpressionAccessStaticLack(node, result);
                    }
                  } else {
                    switch (ast().parent(node.id()).data()) {
                      case Expression.Access _, Expression.Invoke _ -> {
                      }
                      default -> diagnosticExpressionAccessStaticLack(node, result);
                    }
                  }
                }

                @Override
                protected void successImport(Node<Import> node2) {
                  ast().extraUpdate(node2, Extra.Import.class, e -> e.used = true);

                  int resolvedId = ast().extra(node2, Extra.Import.class).resolvedId;
                  if (resolvedId > 0) {
                    ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.resolvedId = resolvedId);
                    switch (ast().parent(node.id()).data()) {
                      case Expression.Access _, Expression.Invoke _ -> {
                      }
                      default -> diagnosticExpressionAccessStaticLack(node, switch (ast().data(resolvedId)) {
                        case Declaration _ -> new Ast.Declaration(resolvedId);
                        case Enumeration _ -> new Ast.Enumeration(resolvedId);
                        default -> throw new AssertionError();
                      });
                    }
                  }
                }
              }.run();
            }
          };

          if (isStatic(node)) {
            impl.runStatic(impl.declaration());
          } else {
            impl.run(new Expression.Type.Declaration(impl.declaration().id()));
          }
        }

        @Override
        protected void successParameter(Node<Parameter> node2) {
          ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.resolvedId = node2.id());

          ast().typeOf(node2.data().definition(ast())).ifPresent(it -> typeSet(node, it));
        }

        @Override
        protected void successStatementVariable(Node<Statement.Variable> node2) {
          ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.resolvedId = node2.id());

          ast().typeOf(node2.data().definition(ast())).ifPresent(it -> typeSet(node, it));
        }

        @Override
        protected void successStatementLoopForEach(Node<Statement.LoopForEach> node2) {
          ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.resolvedId = node2.id());

          ast().typeOf(node2.data().definition(ast())).ifPresent(it -> typeSet(node, it));
        }

        @Override
        protected void successStatementTryCatch(Node<Statement.Try.Catch> node2) {
          ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.resolvedId = node2.id());

          ast().typeOf(node2.data().definition(ast())).ifPresent(it -> typeSet(node, it));
        }
      }.run(node, ast().start(node).index(), node.data().name(ast()));
    } else ast().type(node.data().child(ast()).orElseThrow()).ifPresentOrElse(
      it -> new Impl() {
        @Override
        protected void absent() {
          diagnosticExpressionAccessAbsent(node, it);
        }
      }.run(it),
      () -> {
        // if child is Expression.Access that is resolved to Ast.Pack, Ast.Declaration or Ast.Enumeration -
        // then it's actually a reference
        switch (
          node.data().child(ast())
            .stream()
            .gather(Ast.only(Expression.Access.class))
            .flatMap(it -> ast().extra(it, Extra.ExpressionAccess.class).getResolved(ast()).stream())
            .findFirst()
            .orElse(null)
        ) {
          case Extra.ExpressionAccess.Resolved.Pack v -> {
            // static pack access
            switch (ast().packGet(v.pack(), node.data().name(ast())).orElse(null)) {
              case null -> diagnosticExpressionAccessStaticAbsent(node, v.pack());
              case Ast.Pack v2 -> {
                ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.setResolved(v2));

                if (!(ast().parent(node.id()).data() instanceof Expression.Access)) {
                  diagnosticExpressionAccessStaticLack(node, v2);
                }
              }
              case Ast.PackChild v2 -> {
                ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.setResolved(v2));

                switch (ast().parent(node.id()).data()) {
                  case Expression.Access _, Expression.Invoke _ -> {
                  }
                  default -> diagnosticExpressionAccessStaticLack(node, v2);
                }
              }
            }
          }
          // static declaration access
          case Extra.ExpressionAccess.Resolved.Declaration v -> new Impl() {
            @Override
            protected void absent() {
              diagnosticExpressionAccessStaticAbsentDeclaration(node, v.node(ast()));
            }
          }.runStatic(v.node(ast()));
          // static enumeration access
          case Extra.ExpressionAccess.Resolved.Enumeration v -> v.node(ast())
            .data()
            .children(ast())
            .filter(it -> it.data().nameId() == node.data().nameId())
            .findFirst()
            .ifPresentOrElse(
              it -> {
                ast().extraUpdate(node, Extra.ExpressionAccess.class, e -> e.setResolvedEnumerationChild(it));

                typeSet(node, new Expression.Type.Enumeration(v.id()));
              },
              () -> diagnosticExpressionAccessStaticAbsentEnumeration(node, v.node(ast()))
            );
          case null, default -> {
            // Expression.Access.resolved instanceof Declaration.Field, but Declaration.Field::declaration is unresolved
            // or
            // Expression.Access.resolved instanceof Declaration.Records.Child, but pritimive guid is unresolved
            // or
            // Expression.Access.resolved instanceof Enumeration.Child (type should be set)
            // or
            // Expression.Access.resolved instanceof Parameter
            // or
            // Expression.Access.resolved is variable
            // or
            // Expression.Access.resolved == null
          }
        }
      }
    );
  }

  @Override
  protected void expressionIndex(Node<Expression.Index> node) {
    visit(node.data().childId());

    var typeChild = ast().type(node.data().child(ast())).orElse(null);

    var typeTarget = switch (typeChild) {
      case null -> null;
      case Expression.Type.Array(var value) -> {
        typeSet(node, value);
        yield primaryOf("int").orElse(null);
      }
      case Expression.Type.Map(var key, var value) -> {
        typeSet(node, value);
        yield key;
      }
      default -> {
        diagnosticExpressionIndex(node);
        yield null;
      }
    };

    new VisitAndCast(node.data().value(ast()), typeTarget) {
      @Override
      protected void success(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.ExpressionIndex.class, e -> e.valueCastId = node2.id());
      }
    }.run();
  }

  @Override
  protected void expressionInvoke(Node<Expression.Invoke> node) {
    super.expressionInvoke(node);

    abstract class Impl extends Accessor.Method {
      final List<Node<Expression.Invoke.Argument>> arguments = node.data().arguments(ast()).toList();

      @Override
      protected Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      protected int nameId() {
        return node.data().nameId();
      }

      @Override
      protected Optional<Expression.Type.Declaration> pritimiveOf(String name) {
        return Visitor2.this.primaryOf(name);
      }

      @Override
      protected List<Node<Expression.Invoke.Argument>> arguments() {
        return arguments;
      }

      @Override
      protected void setCast(int index, Node<Declaration.Cast> node) {
        ast().extraUpdate(arguments().get(index), Extra.ExpressionInvokeArgument.class, e -> e.valueCastId = node.id());
      }

      @SuppressWarnings("unchecked")
      @Override
      protected void ambiguous(Stream<? extends Proxy> result) {
        var r = result.toList();
        switch (r.getFirst()) {
          case ProxySynthetic _ ->
            diagnosticExpressionInvokeAmbiguousSynthetic(node, ((List<ProxySynthetic>) r).stream().map(ProxySynthetic::synthetic));
          case ProxyMethod _ -> diagnosticAmbiguous(node, ((List<ProxyMethod>) r).stream().map(ProxyMethod::node));
          case ProxyCast _ -> diagnosticAmbiguous(node, ((List<ProxyCast>) r).stream().map(ProxyCast::node));
        }
      }

      @Override
      protected void successArgumentsInvalid(Proxy result) {
        ast().extraUpdate(node, Extra.ExpressionInvoke.class, e -> e.argumentsInvalid = true);
        success(result);
      }

      @Override
      protected void success(Proxy result) {
        switch (result) {
          case ProxySynthetic v -> {
            ast().extraUpdate(node, Extra.ExpressionInvoke.class, e -> e.resolvedSynthetic = v.synthetic());

            typeSet(node, v.synthetic().result());
          }
          case ProxyMethod v -> {
            ast().extraUpdate(node, Extra.ExpressionInvoke.class, e -> e.setResolvedDeclarationMethod(v.node()));

            ast().typeOf(v.node().data().definition(ast())).ifPresent(it -> typeSet(node, it));
          }
          case ProxyCast v -> {
            ast().extraUpdate(node, Extra.ExpressionInvoke.class, e -> e.setResolvedDeclarationCast(v.node()));

            ast().typeOf(v.node().data().definition(ast())).ifPresent(it -> typeSet(node, it));
          }
        }
      }

      Stream<ProxyMethod> searchStatic(Node<Declaration> node2) {
        return node2.data().children(ast())
          .gather(Ast.only(Declaration.Method.class))
          .filter(it -> it.data().nameId() == nameId())
          // Сразу отбросить методы, не подходящие по количеству переданных аргументов
          .filter(it -> it.data().parameters(ast()).count() == arguments().size())
          .map(ProxyMethod::new)
          .filter(it -> (Declaration.STATIC & modifiers(it)) != 0);
      }

      void runStatic(Node<Declaration> declaration) {
        // little hack
        // skip Protector::run because it's object-context, we are in static context
        // rest of logic is the same
        success(Stream.concat(
          searchStatic(declaration)
            .filter(it -> Declaration.PRIVATE >= protection(it)),
          ast().inheritance(declaration)
            .skip(1)
            .flatMap(this::searchStatic)
            .filter(it -> Declaration.PROTECTED >= protection(it))
        ));
      }
    }

    if (node.data().childId() == 0) {
      if (isStatic(node)) {
        var impl = new Impl() {
          @Override
          protected void absent() {
            diagnosticExpressionInvokeStaticAbsentDeclaration(node, declaration());
          }
        };
        impl.runStatic(impl.declaration());
      } else {
        var impl = new Impl() {
          @Override
          protected void absent() {
            diagnosticExpressionInvokeAbsent(node, new Expression.Type.Declaration(declaration().id()));
          }
        };
        impl.run(new Expression.Type.Declaration(impl.declaration().id()));
      }
    } else ast().type(node.data().child(ast()).orElseThrow()).ifPresentOrElse(
      it -> new Impl() {
        @Override
        protected void absent() {
          diagnosticExpressionInvokeAbsent(node, it);
        }
      }.run(it),
      () -> {
        // if child is Expression.Access that is resolved to Ast.Pack, Ast.Declaration or Ast.Enumeration -
        // then it's actually a reference
        switch (
          node.data().child(ast())
            .stream()
            .gather(Ast.only(Expression.Access.class))
            .flatMap(it -> ast().extra(it, Extra.ExpressionAccess.class).getResolved(ast()).stream())
            .findFirst()
            .orElse(null)
        ) {
          case Extra.ExpressionAccess.Resolved.Declaration v -> new Impl() {
            @Override
            protected void absent() {
              diagnosticExpressionInvokeStaticAbsentDeclaration(node, v.node(ast()));
            }
          }.runStatic(v.node(ast()));
          // static enumeration access
          case Extra.ExpressionAccess.Resolved.Enumeration v ->
            diagnosticExpressionInvokeStaticAbsentEnumeration(node, v.node(ast()));
          case null, default -> {
            // Expression.Access.resolved instanceof Declaration.Field, but Declaration.Field::declaration is unresolved
            // or
            // Expression.Access.resolved instanceof Declaration.Records.Child, but pritimive guid is unresolved
            // or
            // Expression.Access.resolved instanceof Enumeration.Child (type should be set)
            // or
            // Expression.Access.resolved instanceof Ast.Pack
            // or
            // Expression.Access.resolved instanceof Parameter
            // or
            // Expression.Access.resolved is variable
            // or
            // Expression.Access.resolved == null
          }
        }
      }
    );
  }

  @Override
  protected void expressionCast(Node<Expression.Cast> node) {
    var typeTarget = ast().typeOf(node.data().definition(ast())).orElse(null);

    visitHinted(node.data().valueId(), typeTarget);

    var typeSource = ast().type(node.data().value(ast())).orElse(null);

    if (typeTarget != null && typeSource != null) new CasterEnhanced() {
      @Override
      protected Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected void failed() {
        new Caster() {
          @Override
          protected Ast ast() {
            return Visitor2.this.ast();
          }

          @Override
          protected void failed() {
            diagnosticExpressionCastFail(node, typeSource, typeTarget);
          }

          @Override
          protected void success(int distance) {
          }

          @Override
          protected void success() {
            throw new AssertionError();
          }
        }.run(typeTarget, typeSource);
      }

      @Override
      protected void success(int distance) {
        success();
      }

      @Override
      protected void success() {
        diagnosticExpressionCastSuccess(node, typeSource, typeTarget);
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      protected void success(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.ExpressionCast.class, e -> e.valueCastId = node2.id());
      }
    }.run(typeSource, typeTarget);
  }

  @Override
  protected void expressionObject(Node<Expression.Object> node) {
    switch (hint) {
      case null -> diagnosticTypeNoHint(node);
      case Expression.Type.Null _ -> throw new AssertionError();
      case Expression.Type.Declaration _,
           Expression.Type.Enumeration _ -> {
      }
      case Expression.Type.Array _,
           Expression.Type.Map _ -> diagnosticWrong(node, hint);
    }

    typeSet(node, new Expression.Type.Declaration(node.data().child(ast()).id()));

    super.expressionObject(node);
  }

  @Override
  protected void expressionUnary(Node<Expression.Unary> node) {
    super.expressionUnary(node);

    var typeSource = ast().type(node.data().value(ast())).orElse(null);
    if (typeSource == null) return;

    if (!(typeSource instanceof Expression.Type.Declaration)) {
      diagnosticWrong(node.data().value(ast()), typeSource);
      return;
    }

    new Accessor.Operator.Unary() {
      @Override
      protected Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      protected Expression.Type target() {
        return typeSource;
      }

      @Override
      protected Operation operation() {
        return switch (node.data().operation()) {
          case Operation.PLUS -> Operation.ADD;
          case Operation.NEGATE -> Operation.SUBTRACT;
          case Operation.NOT -> Operation.NOT;
          case Operation.BIT_NOT -> Operation.BIT_NOT;
          default -> throw new AssertionError();
        };
      }

      @Override
      protected void absent2() {
        diagnosticAbsent(node, target(), operation());
      }

      @Override
      protected void ambiguous(Stream<? extends Proxy> result) {
        diagnosticAmbiguous(node, result.map(Proxy::node));
      }

      @Override
      protected void success(Proxy result) {
        ast().extraUpdate(node, Extra.ExpressionUnary.class, e -> {
          e.resolvedId = result.node().id();
          e.valueCastId = result.cast().map(Node::id).orElse(0);
        });

        typeSet(node, ast().typeOf(result.node().data().definition(ast())).orElseThrow());
      }
    }.run(typeSource);
  }

  @Override
  protected void expressionBinary(Node<Expression.Binary> node) {
    visit(node.data().leftId());
    var typeLeft = ast().type(node.data().left(ast())).orElse(null);

    expressionBinary(node, typeLeft);
  }

  private void expressionBinary(Node<Expression.Binary> node, Expression.@Nullable Type typeLeft) {
    abstract class Impl extends Accessor.Operator.Assign {
      @Override
      protected final void failedAssign() {
        diagnosticType(node, target(), type());
      }

      @Override
      protected final void success() {
        typeSet(node, target());
      }

      @Override
      protected final void ambiguous(Stream<? extends Node<Declaration.Operator>> result) {
        diagnosticAmbiguous(node, result);
      }

      @Override
      protected final void success(Node<Declaration.Operator> result) {
        ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> e.resolvedAssignId = result.id());

        ast().typeOf(result.data().definition(ast())).ifPresent(it -> typeSet(node, it));
      }

      @Override
      public final void setCast(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> e.rightCastAssignId = node2.id());
      }

      @Override
      protected final Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected final Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }
    }

    switch (node.data().operation()) {
      case Operation.ELVIS -> {
        if (typeLeft != null) {
          typeSet(node, typeLeft);
        }

        new VisitAndCast(node.data().right(ast()), typeLeft) {
          @Override
          protected void success(Node<Declaration.Cast> node2) {
            ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> e.rightCastId = node2.id());
          }
        }.run();
      }

      case Operation.ASSIGN -> {
        visitHinted(node.data().rightId(), typeLeft);
        var typeRight = ast().type(node.data().right(ast())).orElse(null);

        if (typeLeft != null && typeRight != null) new Impl() {
          @Override
          public Expression.Type type() {
            return typeRight;
          }

          @Override
          protected Expression.Type target() {
            return typeLeft;
          }
        }.run(typeLeft);
      }

      case Operation.ASSIGN_ADD,
           Operation.ASSIGN_SUBTRACT,
           Operation.ASSIGN_MULTIPLY,
           Operation.ASSIGN_DIVIDE,
           Operation.ASSIGN_MODULE -> {
        visitHinted(node.data().rightId(), typeLeft);
        var typeRight = ast().type(node.data().right(ast())).orElse(null);

        if (typeLeft != null && typeRight != null) new Accessor.Operator.Binary() {
          @Override
          protected Ast ast() {
            return Visitor2.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return ast().declarationOf(node);
          }

          @Override
          public Expression.Type type() {
            return typeRight;
          }

          @Override
          public void setCast(Node<Declaration.Cast> node2) {
            ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> e.rightCastId = node2.id());
          }

          @Override
          protected Expression.Type target() {
            return typeLeft;
          }

          @Override
          protected Operation operation() {
            return switch (node.data().operation()) {
              case Operation.ASSIGN_ADD -> Operation.ADD;
              case Operation.ASSIGN_SUBTRACT -> Operation.SUBTRACT;
              case Operation.ASSIGN_MULTIPLY -> Operation.MULTIPLY;
              case Operation.ASSIGN_DIVIDE -> Operation.DIVIDE;
              case Operation.ASSIGN_MODULE -> Operation.MODULE;
              default -> throw new AssertionError();
            };
          }

          @SuppressWarnings("unchecked")
          @Override
          protected void ambiguous(Stream<? extends Proxy> result) {
            diagnosticAmbiguous(node, ((Stream<ProxyOperator>) result).map(ProxyOperator::node));
          }

          @Override
          protected void success(Proxy result) {
            var typeOperatorBinary = switch (result) {
              case ProxyOperator v -> {
                ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> {
                  e.resolvedId = v.node().id();
                  e.leftCastId = v.cast().map(Node::id).orElse(0);
                });

                yield ast().typeOf(v.node().data().definition(ast())).orElseThrow();
              }
              case ProxySynthetic v -> {
                ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> e.resolvedSynthetic = v.synthetic());

                yield v.synthetic().result();
              }
            };
            new Impl() {
              @Override
              public Expression.Type type() {
                return typeOperatorBinary;
              }

              @Override
              protected Expression.Type target() {
                return typeLeft;
              }
            }.run(typeLeft);
          }

          @Override
          protected Optional<Expression.Type.Declaration> primaryOf(String name) {
            return Visitor2.this.primaryOf(name);
          }

          @Override
          protected void absent2() {
            diagnosticAbsent(node, target(), operation());
          }
        }.run(typeLeft);
      }

      default -> {
        visit(node.data().rightId());
        var typeRight = ast().type(node.data().right(ast())).orElse(null);

        if (typeLeft != null && typeRight != null) new Accessor.Operator.Binary() {
          @Override
          protected Ast ast() {
            return Visitor2.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return ast().declarationOf(node);
          }

          @Override
          public Expression.Type type() {
            return typeRight;
          }

          @Override
          public void setCast(Node<Declaration.Cast> node2) {
            ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> e.rightCastId = node2.id());
          }

          @Override
          protected Expression.Type target() {
            return typeLeft;
          }

          @Override
          protected Operation operation() {
            return node.data().operation();
          }

          @SuppressWarnings("unchecked")
          @Override
          protected void ambiguous(Stream<? extends Proxy> result) {
            diagnosticAmbiguous(node, ((Stream<ProxyOperator>) result).map(ProxyOperator::node));
          }

          @Override
          protected void success(Proxy result) {
            switch (result) {
              case ProxyOperator v -> {
                ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> {
                  e.resolvedId = v.node().id();
                  e.leftCastId = v.cast().map(Node::id).orElse(0);
                });

                ast().typeOf(v.node().data().definition(ast())).ifPresent(it -> typeSet(node, it));
              }
              case ProxySynthetic v -> {
                ast().extraUpdate(node, Extra.ExpressionBinary.class, e -> e.resolvedSynthetic = v.synthetic());

                typeSet(node, v.synthetic().result());
              }
              default -> throw new AssertionError();
            }
          }

          @Override
          protected Optional<Expression.Type.Declaration> primaryOf(String name) {
            return Visitor2.this.primaryOf(name);
          }

          @Override
          protected void absent2() {
            diagnosticAbsent(node, target(), operation());
          }
        }.run(typeLeft);
      }
    }
  }

  @Override
  protected void expressionInstanceof(Node<Expression.Instanceof> node) {
    var typeTarget = ast().typeOf(node.data().definition(ast())).orElse(null);

    visitHinted(node.data().valueId(), typeTarget);

    var typeSource = ast().type(node.data().value(ast())).orElse(null);
    if (typeTarget == null || typeSource == null) return;

    new Caster() {
      @Override
      protected Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected void failed() {
        new Caster() {
          @Override
          protected Ast ast() {
            return Visitor2.this.ast();
          }

          @Override
          protected void failed() {
            diagnosticExpressionInstanceofFail(node, typeSource, typeTarget);
          }

          @Override
          protected void success(int distance) {
          }

          @Override
          protected void success() {
            throw new AssertionError();
          }
        }.run(typeTarget, typeSource);
      }

      @Override
      protected void success(int distance) {
        success();
      }

      @Override
      protected void success() {
        diagnosticExpressionInstanceofSuccess(node);
      }
    }.run(typeSource, typeTarget);
  }

  @Override
  protected void expressionTernary(Node<Expression.Ternary> node) {
    var typeBool = primaryOf("bool").orElse(null);

    new VisitAndCast(node.data().condition(ast()), typeBool) {
      @Override
      protected void success(Node<Declaration.Cast> node2) {
        ast().extraUpdate(node, Extra.ExpressionTernary.class, e -> e.conditionCastId = node2.id());
      }
    }.run();

    visit(node.data().thenId());
    var typeThen = ast().type(node.data().then(ast())).orElse(null);

    visit(node.data().otherId());
    var typeOther = ast().type(node.data().other(ast())).orElse(null);

    if (typeThen != null && typeOther != null) new CasterEnhanced() {
      @Override
      protected Ast ast() {
        return Visitor2.this.ast();
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      protected void failed() {
        new CasterEnhanced() {
          @Override
          protected Ast ast() {
            return Visitor2.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return ast().declarationOf(node);
          }

          @Override
          protected void failed() {
            diagnosticType(node, typeThen, typeOther);
          }

          @Override
          protected void success(int distance) {
            success();
          }

          @Override
          protected void success() {
            typeSet(node, typeOther);
          }

          @Override
          protected void success(Node<Declaration.Cast> node2) {
            success();

            ast().extraUpdate(node, Extra.ExpressionTernary.class, e -> e.thenCastId = node2.id());
          }
        }.run(typeThen, typeOther);
      }

      @Override
      protected void success(int distance) {
        success();
      }

      @Override
      protected void success() {
        typeSet(node, typeThen);
      }

      @Override
      protected void success(Node<Declaration.Cast> node2) {
        success();

        ast().extraUpdate(node, Extra.ExpressionTernary.class, e -> e.otherCastId = node2.id());
      }
    }.run(typeOther, typeThen);
  }

  @Override
  protected void expressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
    if (hint instanceof Expression.Type.Array(var value)) {
      new VisitAndCast(node.data().value(ast()), value) {
        @Override
        protected void success(Node<Declaration.Cast> node2) {
          ast().extraUpdate(node, Extra.ExpressionArrayChildValue.class, e -> e.valueCastId = node2.id());
        }
      }.run();
    } else {
      super.expressionArrayChildValue(node);
    }
  }

  @Override
  protected void expressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
    if (hint instanceof Expression.Type.Map(var key, var value)) {
      new VisitAndCast(node.data().key(ast()), key) {
        @Override
        protected void success(Node<Declaration.Cast> node2) {
          ast().extraUpdate(node, Extra.ExpressionArrayChildEntry.class, e -> e.keyCastId = node2.id());
        }
      }.run();

      new VisitAndCast(node.data().value(ast()), value) {
        @Override
        protected void success(Node<Declaration.Cast> node2) {
          ast().extraUpdate(node, Extra.ExpressionArrayChildEntry.class, e -> e.valueCastId = node2.id());
        }
      }.run();
    } else {
      super.expressionArrayChildEntry(node);
    }
  }

  private abstract class VisitAndCast extends CasterEnhanced {
    private final Node<? extends Expression> node;
    private final Expression.@Nullable Type target;

    protected VisitAndCast(Node<? extends Expression> node, Expression.@Nullable Type target) {
      this.node = node;
      this.target = target;
    }

    @Override
    protected final Ast ast() {
      return Visitor2.this.ast();
    }

    @Override
    protected final Node<Declaration> declaration() {
      return ast().declarationOf(node);
    }

    @Override
    protected final void failed() {
      assert target != null;
      diagnosticType(node, target, ast().type(node).orElseThrow());
    }

    @Override
    protected final void success(int distance) {
    }

    @Override
    protected final void success() {
    }

    final void run() {
      visitHinted(node.id(), target);
      if (target != null) {
        ast().type(node).ifPresent(it -> run(it, target));
      }
    }
  }
}
