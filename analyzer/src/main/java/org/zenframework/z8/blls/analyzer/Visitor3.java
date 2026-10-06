package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Шаг 3:
/// - Отсеять повторяющиеся включения
/// - Отсеять повторяющиеся аннотации
/// - Отсеять повторяющиеся параметры
/// - Отсеять повторяющиеся поля
/// - Отсеять повторяющиеся методы
/// - Отсеять повторяющиеся операторы приоритета
/// - Отсеять повторяющиеся операторы приведения
/// - Отсеять повторяющиеся унарные/бинарные операторы
/// - Отсеять повторяющиеся записи
/// - Отсеять повторяющиеся значения перечислений
/// - Отсеять повторяющиеся переменные в блоках
/// - Проверить аннотации
/// - Проверить модификаторы
/// - Проверить параметры
/// - Проверить наличие тела у метода
/// - Проверить наличие тела у оператора приоритета
/// - Проверить наличие тела у оператора приведения
/// - Проверить наличие тела у унарных/бинарных операторов
/// - Проверить параметры операторов
/// - Не наследовать перечисления
/// - Не наследовать final-классы
/// - Не допускать циклов в наследовании
/// - Проверить модификатор auto у полей
/// - Проверить модификатор final у полей
/// - Проверить переопределяемые методы
/// - Проверить переопределяемые операторы приоритета
/// - Проверить переопределяемые операторы приведения
/// - Проверить переопределяемые унарные/бинарные операторы
/// - Разрешить Statement.Break
/// - Разрешить Statement.Continue
/// - Проверить наличие operator= у классов, наследующих primary
/// - Проверить достижимость кода
/// - Проверить возврат значения на всех путях
/// - Проверить достижимость catch-блоков
public abstract class Visitor3 extends Visitor {
  private int controlFlowNodeId = -1;
  private int controlFlowNodeIdOld = -1;
  private boolean controlFlowUsed = false;

  @Override
  protected abstract Ast ast();

  protected abstract Optional<Expression.Type.Declaration> primaryOf(String name);

  /// Повторное объявление
  protected abstract void diagnosticDuplicate(Node<?> nodeOld, Node<?> nodeNew);

  private void diagnosticDuplicate(Ast.Duplicate duplicate) {
    diagnosticDuplicate(ast().node(duplicate.oldId()), ast().node(duplicate.newId()));
  }

  /// Ожидается `target`: дано `source`
  protected abstract void diagnosticType(Node<?> node, Expression.Type target, Expression.Type source);

  /// Недопустимые модификаторы
  protected abstract void diagnosticModifiers(Node<?> node, int modifiers);

  /// Неизвестный атрибут
  protected abstract void diagnosticAnnotationUnknown(Node<Annotation> node);

  /// Не ожидается значение
  protected abstract void diagnosticAnnotationExpectNone(Node<Annotation> node);

  /// Ожидается true/false
  protected abstract void diagnosticAnnotationExpectBoolean(Node<Annotation> node);

  /// Ожидается строка
  protected abstract void diagnosticAnnotationExpectString(Node<Annotation> node);

  /// Ожидается значение: `regex`
  protected abstract void diagnosticAnnotationExpectString(Node<Annotation> node, Pattern regex);

  /// Ожидается выражение CRON
  protected abstract void diagnosticAnnotationExpectCron(Node<Annotation> node);

  /// Недопустимые параметры
  protected abstract void diagnosticParameters(Node<? extends Declaration.Child> node);

  /// `parent`.name: нельзя наследовать final класс
  protected abstract void diagnosticDeclarationExtendsFinal(Node<Declaration> node, Node<Klass> extended);

  /// `parent`.name: нельзя наследовать перечисление
  protected abstract void diagnosticDeclarationExtendsEnumeration(Node<Declaration> node, Node<Enumeration> extended);

  /// Нельзя наследовать массив или словарь
  protected abstract void diagnosticDeclarationExtendsTypeWrong(Node<Declaration> node, Expression.Type type);

  /// Цикл в наследовании
  protected abstract void diagnosticDeclarationExtendsCycle(Stream<Node<Klass>> cycle);

  /// Наследование класса `primary`.name с атрибутом primary
  protected abstract void diagnosticDeclarationPrimaryConditions(Node<Declaration> node, Node<Klass> primary);

  /// Переопределение невозможно: родительское объявление не виртуальное
  protected abstract void diagnosticDeclarationChildOverride(Node<? extends Declaration.Child> node, Node<? extends Declaration.Child> extended);

  /// Не все пути возвращают значение
  protected abstract void diagnosticDeclarationChildReturns(Node<? extends Declaration.Child> node);

  /// Требуется тело
  protected abstract void diagnosticDeclarationChildUnimplemented(Node<? extends Declaration.Child> node);

  /// auto: ожидается массив значений неосновного типа
  protected abstract void diagnosticDeclarationFieldModifierAuto(Node<Declaration.Field> node);

  /// static: применим только к основным типам или массивам основных типов
  protected abstract void diagnosticDeclarationFieldModifierStatic(Node<Declaration.Field> node);

  /// final: требуется значение
  protected abstract void diagnosticDeclarationFieldModifierFinal(Node<Declaration.Field> node);

  /// Атрибуты применимы только к полям неосновных типов
  protected abstract void diagnosticDeclarationInitializeAnnotation(Node<Declaration.Initialize> node);

  /// Перекрывается `shadowedBy`
  protected abstract void diagnosticStatementVariable(Node<Statement.Variable> node, Node<Statement.Variable> shadowedBy);

  /// Вне цикла
  protected abstract void diagnosticStatementBreak(Node<Statement.Break> node);

  /// Вне цикла
  protected abstract void diagnosticStatementContinue(Node<Statement.Continue> node);

  /// Недостижимый код
  protected abstract void diagnosticStatementUnreachable(Node<? extends Statement> node);

  /// Недостижимый код
  protected abstract void diagnosticStatementTryCatchUnreachable(Node<Statement.Try.Catch> node, Node<Statement.Try.Catch> shadowedBy);

  /// Класс-выражение поддерживается только в полях и переменных
  protected abstract void diagnosticExpressionObject(Node<Expression.Object> node);

  private void checkModifiers(Node<?> node, int modifiers, int expected) {
    boolean diagnostic = false;

    // Отсутствуют повторяющиеся модификаторы доступа
    int protectionDuplicates = modifiers & (Declaration.PUBLIC | Declaration.PROTECTED | Declaration.PRIVATE);
    if (Integer.bitCount(protectionDuplicates) > 1) {
      diagnosticModifiers(node, protectionDuplicates);
      diagnostic = true;
    }

    // Все модификаторы перечислены в expected
    int disallowed = modifiers & ~expected;
    if (disallowed != 0) {
      if (!diagnostic)
        diagnosticModifiers(node, disallowed);
    }

    if ((Declaration.STATIC & modifiers) != 0 && (Declaration.VIRTUAL & modifiers) != 0) {
      // static и virtual - взаимоисключающие
      if (!diagnostic)
        diagnosticModifiers(node, Declaration.STATIC | Declaration.VIRTUAL);
    } else if ((Declaration.PRIVATE & modifiers) != 0 && (Declaration.VIRTUAL & modifiers) != 0) {
      if (!diagnostic)
        diagnosticModifiers(node, Declaration.PRIVATE | Declaration.VIRTUAL);
    } else if (
      (Declaration.STATIC & modifiers) != 0 &&
        node.data() instanceof Declaration.Child &&
        ast().parents(node.id())
          .skip(2)
          .mapToObj(ast()::node)
          .findFirst()
          .orElseThrow()
          .data()
          instanceof Expression.Object
    ) {
      // static внутри Expression.Object - недопустимо
      if (!diagnostic)
        diagnosticModifiers(node, Declaration.STATIC);
    }
  }

  private void checkBody(Node<? extends Declaration.Child> node) {
    // Объявления без тела допускаются только для классов с атрибутами primary или native
    var d = ast().declarationOf(node);
    if (ast().parent(d.id()).data() instanceof Expression.Object || !ast().isNative(d)) {
      // Если внутри класса-выражения или не-primary, не-native - сообщить
      diagnosticDeclarationChildUnimplemented(node);
    }
  }

  private void checkControlFlow(Node<? extends Declaration.Child> node, Expression.@Nullable Type typeTarget, Node<Statement.Block> block) {
    int cf = visitControlFlowNodeId(block);
    if (typeTarget == null) return;

    var typeVoid = primaryOf("void").orElse(null);
    if (typeVoid == null) return;

    if (!typeTarget.equals(typeVoid) && cf == 0)
      diagnosticDeclarationChildReturns(node);
  }

  private OptionalInt closestLoop(int id) {
    var closest = ast().parents(id)
      .mapToObj(ast()::node)
      .filter(it -> switch (it.data()) {
        case Declaration.Child _,
             Statement.LoopWhile _,
             Statement.LoopFor _,
             Statement.LoopForEach _ -> true;
        default -> false;
      })
      .findFirst()
      .orElseThrow();

    return switch (closest.data()) {
      case Declaration.Child _ -> OptionalInt.empty();
      case Statement.LoopWhile _,
           Statement.LoopFor _,
           Statement.LoopForEach _ -> OptionalInt.of(closest.id());
      default -> throw new AssertionError();
    };
  }

  private int closestParent(int aId, int bId) {
    return ast().parents(aId).anyMatch(it -> it == bId) ? aId : bId;
  }

  private void setControlFlowNodeId(int valueId) {
    this.controlFlowNodeIdOld = valueId;
  }

  private int visitControlFlowNodeId(Node<? extends Statement> node) {
    this.controlFlowUsed = true;
    visit(node.id());
    assert this.controlFlowNodeId >= 0;
    return this.controlFlowNodeId;
  }

  @Override
  public void visit(int id) {
    boolean controlFlowUsed = this.controlFlowUsed;
    this.controlFlowUsed = false;

    super.visit(id);

    this.controlFlowNodeId = this.controlFlowNodeIdOld;
    this.controlFlowNodeIdOld = -1;
    assert controlFlowUsed == (this.controlFlowNodeId >= 0);
  }

  @Override
  protected void source(Node<Source> node) {
    node.data().imports(ast())
      .gather(Ast.duplicatesInt(it -> it.value(ast()).data().nameId()))
      .forEach(this::diagnosticDuplicate);

    super.source(node);
  }

  @Override
  protected void klass(Node<Klass> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    checkModifiers(node, node.data().modifiers(), Declaration.PUBLIC | Declaration.FINAL);

    {
      var visited = new LinkedHashSet<Node<Klass>>();
      visited.add(node);
      for (
        var n = node.data().child(ast());
        ast().extendedOf(n) instanceof Expression.Type.Declaration v;
        n = v.node(ast())
      ) {
        var klass = ast().<Klass>parent(v.id());
        if (!visited.add(klass)) {
          // Цикл может быть в части дерева наследования, но сообщение выводится только при полноценном цикле
          // Поэтому проверяется, что посещаемый класс закрывает цикл
          // Visitor посетит все классы, и эта проверка будет сделана для каждого
          if (klass.equals(node))
            diagnosticDeclarationExtendsCycle(visited.stream());
          break;
        }
      }
    }

    super.klass(node);
  }

  @Override
  protected void enumeration(Node<Enumeration> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    checkModifiers(node, node.data().modifiers(), Declaration.PUBLIC);

    node.data().children(ast())
      .gather(Ast.duplicatesInt(Enumeration.Child::nameId))
      .forEach(this::diagnosticDuplicate);

    super.enumeration(node);
  }

  @Override
  protected void annotation(Node<Annotation> node) {
    if (ast().parent(node.id()).data() instanceof Declaration.Records.Child) {
      super.annotation(node);
      return;
    }

    switch (node.data().name(ast())) {
      case Annotation.NAME -> node.data().value(ast()).ifPresentOrElse(
        it -> {
          var typeSource = ast().type(it).orElse(null);
          var typeTarget = primaryOf("string").orElse(null);
          if (typeSource != null && typeTarget != null && !Caster.runSimple(ast(), typeSource, typeTarget))
            diagnosticAnnotationExpectString(node);
          if (it.data() instanceof Expression.String v2 && !Annotation.REGEX_NAME.matcher(v2.value(ast())).matches())
            diagnosticAnnotationExpectString(node, Annotation.REGEX_NAME);
        },
        () -> diagnosticAnnotationExpectString(node)
      );
      case Annotation.JOB -> node.data().value(ast()).ifPresent(it -> {
        if (!(it.data() instanceof Expression.String v))
          diagnosticAnnotationExpectString(node);
        else if (!Annotation.REGEX_CRON.matcher(v.value(ast())).matches())
          diagnosticAnnotationExpectCron(node);
      });
      case Annotation.ACTIVATOR,
           Annotation.ENTRY,
           Annotation.LOG,
           Annotation.GENERATABLE,
           Annotation.EXECUTABLE -> {
        if (node.data().valueId() > 0)
          diagnosticAnnotationExpectNone(node);
      }
      case Annotation.ICON,
           Annotation.DISPLAY_NAME -> node.data().value(ast()).ifPresent(it -> {
        var typeSource = ast().type(it).orElse(null);
        var typeTarget = primaryOf("string").orElse(null);
        if (typeSource != null && typeTarget != null && !Caster.runSimple(ast(), typeSource, typeTarget))
          diagnosticAnnotationExpectString(node);
      });
      case Annotation.PRIMARY,
           Annotation.NATIVE,
           Annotation.DEPRECATED,
           Annotation.UI -> node.data().value(ast()).ifPresentOrElse(
        it -> {
          var typeSource = ast().type(it).orElse(null);
          var typeTarget = primaryOf("string").orElse(null);
          if (typeSource != null && typeTarget != null && !Caster.runSimple(ast(), typeSource, typeTarget))
            diagnosticAnnotationExpectString(node);
        },
        () -> diagnosticAnnotationExpectString(node)
      );
      case Annotation.REQUEST -> node.data().value(ast()).ifPresent(it -> {
        var typeSource = ast().type(it).orElse(null);
        var typeTarget = primaryOf("boolean").orElse(null);
        if (typeSource != null && typeTarget != null && !Caster.runSimple(ast(), typeSource, typeTarget))
          diagnosticAnnotationExpectString(node);
      });
      case Annotation.ABSTRACT,
           Annotation.FOREIGN_KEY -> node.data().value(ast()).ifPresentOrElse(
        it -> {
          var typeSource = ast().type(it).orElse(null);
          var typeTarget = primaryOf("boolean").orElse(null);
          if (typeSource != null && typeTarget != null && !Caster.runSimple(ast(), typeSource, typeTarget))
            diagnosticAnnotationExpectBoolean(node);
        },
        () -> diagnosticAnnotationExpectBoolean(node)
      );
      case Annotation.LOCK_KEY -> {
      }
      default -> diagnosticAnnotationUnknown(node);
    }

    super.annotation(node);
  }

  @Override
  protected void declaration(Node<Declaration> node) {
    node.data().children(ast())
      .gather(Ast.only(Declaration.Field.class))
      .gather(Ast.duplicatesInt(Declaration.Field::nameId))
      .forEach(this::diagnosticDuplicate);

    node.data().children(ast())
      .gather(Ast.only(Declaration.Method.class))
      .filter(it -> Extra.DeclarationMethod.Key.of(ast(), it).isPresent())
      .gather(Ast.duplicates(it -> Extra.DeclarationMethod.Key.of(ast(), it).orElseThrow()))
      .forEach(this::diagnosticDuplicate);

    {
      var iter = node.data().children(ast())
        .gather(Ast.only(Declaration.Priority.class))
        .iterator();
      if (iter.hasNext()) {
        var priority = iter.next();
        iter.forEachRemaining(it -> diagnosticDuplicate(priority, it));
      }
    }

    node.data().children(ast())
      .gather(Ast.only(Declaration.Cast.class))
      .filter(it -> ast().typeOf(it.data().definition(ast())).isPresent())
      .gather(Ast.duplicates(it -> ast().typeOf(it.data().definition(ast())).orElseThrow()))
      .forEach(this::diagnosticDuplicate);

    node.data().children(ast())
      .gather(Ast.only(Declaration.Operator.class))
      .filter(it -> Extra.DeclarationOperator.Key.of(ast(), it).isPresent())
      .gather(Ast.duplicates(it -> Extra.DeclarationOperator.Key.of(ast(), it).orElseThrow()))
      .forEach(this::diagnosticDuplicate);

    {
      var recordsIter = node.data().children(ast())
        .gather(Ast.only(Declaration.Records.class))
        .iterator();
      if (recordsIter.hasNext()) {
        var records = recordsIter.next();
        recordsIter.forEachRemaining(it -> diagnosticDuplicate(records, it));

        var iter = records.data().children(ast()).iterator();
        while (iter.hasNext()) {
          var record = iter.next();
          node.data().children(ast())
            .gather(Ast.only(Declaration.Field.class))
            .filter(it -> it.data().nameId() == record.data().nameId())
            .findFirst()
            .ifPresent(it -> diagnosticDuplicate(it, record));
        }
      }
    }

    if (!ast().isNative(node)) ast().inheritanceKlasses(node)
      .filter(it -> ast().annotationOf(it, Annotation.PRIMARY).isPresent())
      .findFirst()
      .ifPresent(primary -> {
        var key = new Extra.DeclarationOperator.Key(Operation.ASSIGN, new Expression.Type.Declaration(node.id()));
        boolean operatorAssignPrimaryAbsent = node.data()
          .children(ast())
          .gather(Ast.only(Declaration.Operator.class))
          .filter(it -> key.equals(Extra.DeclarationOperator.Key.of(ast(), it).orElse(null)))
          .findFirst()
          .isEmpty();

        if (operatorAssignPrimaryAbsent) {
          diagnosticDeclarationPrimaryConditions(node, primary);
        }
      });

    switch (ast().extendedOf(node)) {
      case null -> {
      }
      case Expression.Type.Null _ -> throw new AssertionError();
      case Expression.Type.Declaration v -> {
        var klass = ast().<Klass>parent(v.id());
        if ((Declaration.FINAL & klass.data().modifiers()) != 0)
          diagnosticDeclarationExtendsFinal(node, klass);
      }
      case Expression.Type.Enumeration v -> diagnosticDeclarationExtendsEnumeration(node, v.node(ast()));
      case Expression.Type.Array v -> diagnosticDeclarationExtendsTypeWrong(node, v);
      case Expression.Type.Map v -> diagnosticDeclarationExtendsTypeWrong(node, v);
    }

    super.declaration(node);
  }

  @Override
  protected void declarationField(Node<Declaration.Field> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    checkModifiers(node, node.data().modifiers(),
      Declaration.PUBLIC |
        Declaration.PROTECTED |
        Declaration.PRIVATE |
        Declaration.STATIC |
        Declaration.FINAL |
        Declaration.AUTO
    );

    var type = ast().typeOf(node.data().definition(ast())).orElse(null);

    if ((Declaration.AUTO & node.data().modifiers()) != 0) {
      // Модификатор auto применяется к полям
      boolean validAuto =
        // Типа T[]
        type instanceof Expression.Type.Array(var child) &&
          // Где T - ссылка
          child instanceof Expression.Type.Declaration v &&
          // На тип не являющийся основным
          // (т.е. int[] - нет; Object[] - да)
          !ast().isPrimary(v.node(ast()));
      if (!validAuto) {
        diagnosticDeclarationFieldModifierAuto(node);
      }
    }

    if (
      (Declaration.STATIC & node.data().modifiers()) != 0 &&
        type != null &&
        (ast().isReference(type) || ast().isArrayOfReferences(type))
    ) {
      diagnosticDeclarationFieldModifierStatic(node);
    }

    if (
      (Declaration.FINAL & node.data().modifiers()) != 0 &&
        node.data().valueId() == 0
        && !ast().isNative(ast().declarationOf(node))
    ) {
      diagnosticDeclarationFieldModifierFinal(node);
    }

    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().definitionId());
    if (node.data().valueId() > 0) {
      int cf = visitControlFlowNodeId(node.data().value(ast()).orElseThrow());
      assert cf == 0;
    }
  }

  @Override
  protected void declarationInitialize(Node<Declaration.Initialize> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    var typeChild = ast().initializeAccesses(node.data())
      .findFirst()
      .flatMap(it -> ast().type(it))
      .orElse(null);
    if (typeChild != null && !ast().isReference(typeChild) && node.data().annotationsId() > 0)
      diagnosticDeclarationInitializeAnnotation(node);

    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    int cf = visitControlFlowNodeId(node.data().child(ast()));
    assert cf == 0;
  }

  @Override
  protected void declarationMethod(Node<Declaration.Method> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    checkModifiers(node, node.data().modifiers(),
      Declaration.PUBLIC |
        Declaration.PROTECTED |
        Declaration.PRIVATE |
        Declaration.STATIC |
        Declaration.VIRTUAL
    );

    node.data().parameters(ast())
      .gather(Ast.duplicatesInt(Parameter::nameId))
      .forEach(this::diagnosticDuplicate);

    Extra.DeclarationMethod.Key.of(ast(), node).ifPresent(key -> new CheckOverride<Node<Declaration.Method>>() {
      @Override
      protected int modifiers(Node<Declaration.Method> value) {
        return value.data().modifiers() & (
          Declaration.PUBLIC |
            Declaration.PROTECTED |
            Declaration.PRIVATE |
            Declaration.STATIC |
            Declaration.VIRTUAL
        );
      }

      @Override
      protected void success(Node<Declaration.Method> result) {
        if ((Declaration.VIRTUAL & result.data().modifiers()) == 0) {
          diagnosticDeclarationChildOverride(node, result);
        } else {
          var typeA = ast().typeOf(node.data().definition(ast())).orElse(null);
          var typeB = ast().typeOf(result.data().definition(ast())).orElse(null);
          if (typeA == null || typeB == null) return;

          if (typeA.equals(typeB))
            ast().extraUpdate(node, Extra.DeclarationMethod.class, e -> e.overrideId = result.id());
          else
            diagnosticType(node, typeB, typeA);
        }
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      protected Stream<? extends Node<Declaration.Method>> search(Node<Declaration> node2) {
        return node2.data()
          .children(ast())
          .gather(Ast.only(Declaration.Method.class))
          .filter(it -> key.equals(Extra.DeclarationMethod.Key.of(ast(), it).orElse(null)));
      }
    }.run());

    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().definitionId());
    ast().siblings(node.data().parametersId()).forEach(this::visit);
    if (node.data().blockId() == 0) checkBody(node);
    else checkControlFlow(
      node,
      ast().typeOf(node.data().definition(ast())).orElse(null),
      node.data().block(ast()).orElseThrow()
    );
  }

  @Override
  protected void declarationPriority(Node<Declaration.Priority> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    checkModifiers(node, node.data().modifiers(), Declaration.PUBLIC | Declaration.VIRTUAL);

    if (node.data().parameters(ast()).findAny().isPresent())
      diagnosticParameters(node);

    new CheckOverride<Node<Declaration.Priority>>() {
      @Override
      protected int modifiers(Node<Declaration.Priority> value) {
        return value.data().modifiers() & (Declaration.PUBLIC | Declaration.VIRTUAL);
      }

      @Override
      protected void success(Node<Declaration.Priority> result) {
        if ((Declaration.VIRTUAL & result.data().modifiers()) == 0)
          diagnosticDeclarationChildOverride(node, result);
        else
          ast().extraUpdate(node, Extra.DeclarationPriority.class, e -> e.overrideId = result.id());
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      protected Stream<? extends Node<Declaration.Priority>> search(Node<Declaration> node2) {
        return node2.data()
          .children(ast())
          .gather(Ast.only(Declaration.Priority.class));
      }
    }.run();

    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    ast().siblings(node.data().parametersId()).forEach(this::visit);
    if (node.data().blockId() == 0) checkBody(node);
    else checkControlFlow(
      node,
      new Expression.Type.Declaration(ast().declarationOf(node).id()),
      node.data().block(ast()).orElseThrow()
    );
  }

  @Override
  protected void declarationCast(Node<Declaration.Cast> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    checkModifiers(node, node.data().modifiers(), Declaration.PUBLIC | Declaration.VIRTUAL);

    if (node.data().parameters(ast()).findAny().isPresent())
      diagnosticParameters(node);

    ast().typeOf(node.data().definition(ast())).ifPresent(type -> new CheckOverride<Node<Declaration.Cast>>() {
      @Override
      protected int modifiers(Node<Declaration.Cast> value) {
        return value.data().modifiers() & (Declaration.PUBLIC | Declaration.VIRTUAL);
      }

      @Override
      protected void success(Node<Declaration.Cast> result) {
        if ((Declaration.VIRTUAL & result.data().modifiers()) == 0) {
          diagnosticDeclarationChildOverride(node, result);
        } else {
          ast().extraUpdate(node, Extra.DeclarationCast.class, e -> e.overrideId = result.id());
        }
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      protected Stream<? extends Node<Declaration.Cast>> search(Node<Declaration> node2) {
        return node2.data()
          .children(ast())
          .gather(Ast.only(Declaration.Cast.class))
          .filter(it -> type.equals(ast().typeOf(it.data().definition(ast())).orElse(null)));
      }
    }.run());

    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().definitionId());
    ast().siblings(node.data().parametersId()).forEach(this::visit);
    if (node.data().blockId() == 0) checkBody(node);
    else checkControlFlow(
      node,
      ast().typeOf(node.data().definition(ast())).orElse(null),
      node.data().block(ast()).orElseThrow()
    );
  }

  @Override
  protected void declarationOperator(Node<Declaration.Operator> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    checkModifiers(node, node.data().modifiers(), Declaration.PUBLIC | Declaration.VIRTUAL);

    long parametersCount = node.data().parameters(ast()).count();
    boolean parametersValid = switch (node.data().operation()) {
      case NOT, BIT_NOT -> parametersCount == 0;

      case ADD,
           SUBTRACT -> parametersCount <= 1;

      case MULTIPLY,
           DIVIDE,
           MODULE,
           LESS,
           LESS_EQUAL,
           MORE,
           MORE_EQUAL,
           EQUAL,
           NOT_EQUAL,
           AND,
           OR,
           BIT_AND,
           BIT_OR,
           BIT_XOR,
           ASSIGN -> parametersCount == 1;

      case PLUS,
           NEGATE,
           ELVIS,
           ASSIGN_ADD,
           ASSIGN_SUBTRACT,
           ASSIGN_MULTIPLY,
           ASSIGN_DIVIDE,
           ASSIGN_MODULE -> throw new AssertionError();
    };
    if (!parametersValid)
      diagnosticParameters(node);

    Extra.DeclarationOperator.Key.of(ast(), node).ifPresent(key -> new CheckOverride<Node<Declaration.Operator>>() {
      @Override
      protected int modifiers(Node<Declaration.Operator> value) {
        return value.data().modifiers() & (Declaration.PUBLIC | Declaration.VIRTUAL);
      }

      @Override
      protected void success(Node<Declaration.Operator> result) {
        if ((Declaration.VIRTUAL & result.data().modifiers()) == 0) {
          diagnosticDeclarationChildOverride(node, result);
        } else {
          var typeA = ast().typeOf(node.data().definition(ast())).orElse(null);
          var typeB = ast().typeOf(result.data().definition(ast())).orElse(null);
          if (typeA == null || typeB == null) return;

          if (typeA.equals(typeB))
            ast().extraUpdate(node, Extra.DeclarationOperator.class, e -> e.overrideId = result.id());
          else
            diagnosticType(node, typeB, typeA);
        }
      }

      @Override
      protected Node<Declaration> declaration() {
        return ast().declarationOf(node);
      }

      @Override
      protected Stream<? extends Node<Declaration.Operator>> search(Node<Declaration> node2) {
        return node2.data()
          .children(ast())
          .gather(Ast.only(Declaration.Operator.class))
          .filter(it -> key.equals(Extra.DeclarationOperator.Key.of(ast(), it).orElse(null)));
      }
    }.run());

    ast().siblings(node.data().annotationsId()).forEach(this::visit);
    visit(node.data().definitionId());
    ast().siblings(node.data().parametersId()).forEach(this::visit);
    if (node.data().blockId() == 0) checkBody(node);
    else checkControlFlow(
      node,
      ast().typeOf(node.data().definition(ast())).orElse(null),
      node.data().block(ast()).orElseThrow()
    );
  }

  @Override
  protected void declarationRecords(Node<Declaration.Records> node) {
    node.data().children(ast())
      .gather(Ast.duplicatesInt(Declaration.Records.Child::nameId))
      .forEach(this::diagnosticDuplicate);

    super.declarationRecords(node);
  }

  @Override
  protected void declarationRecordsChild(Node<Declaration.Records.Child> node) {
    node.data().annotations(ast())
      .gather(Ast.duplicatesInt(Annotation::nameId))
      .forEach(this::diagnosticDuplicate);

    checkModifiers(node, node.data().modifiers(), Declaration.PUBLIC | Declaration.PROTECTED | Declaration.PRIVATE);

    super.declarationRecordsChild(node);
  }

  @Override
  protected void statementBlock(Node<Statement.Block> node) {
    node.data().statements(ast())
      .gather(Ast.only(Statement.Variable.class))
      .gather(Ast.duplicatesInt(Statement.Variable::nameId))
      .forEach(it -> diagnosticStatementVariable(ast().node(it.newId()), ast().node(it.oldId())));

    int cf = 0;
    var iter = node.data().statements(ast()).iterator();
    while (iter.hasNext()) {
      cf = visitControlFlowNodeId(iter.next());

      if (cf > 0) {
        if (iter.hasNext())
          diagnosticStatementUnreachable(iter.next());

        break;
      }
    }

    while (iter.hasNext()) {
      int _ = visitControlFlowNodeId(iter.next());
    }

    setControlFlowNodeId(cf);
  }

  @Override
  protected void statementExpression(Node<Statement.Expression> node) {
    super.statementExpression(node);

    setControlFlowNodeId(0);
  }

  @Override
  protected void statementVariable(Node<Statement.Variable> node) {
    super.statementVariable(node);

    setControlFlowNodeId(0);
  }

  @Override
  protected void statementReturn(Node<Statement.Return> node) {
    super.statementReturn(node);

    setControlFlowNodeId(
      ast().parents(node.id())
        .filter(it -> ast().data(it) instanceof Declaration.Child)
        .findFirst()
        .orElseThrow()
    );
  }

  @Override
  protected void statementBreak(Node<Statement.Break> node) {
    super.statementBreak(node);

    closestLoop(node.id()).ifPresentOrElse(
      this::setControlFlowNodeId,
      () -> {
        diagnosticStatementBreak(node);
        setControlFlowNodeId(0);
      }
    );
  }

  @Override
  protected void statementContinue(Node<Statement.Continue> node) {
    super.statementContinue(node);

    closestLoop(node.id()).ifPresentOrElse(
      this::setControlFlowNodeId,
      () -> {
        diagnosticStatementContinue(node);
        setControlFlowNodeId(0);
      }
    );
  }

  @Override
  protected void statementThrow(Node<Statement.Throw> node) {
    super.statementThrow(node);

    setControlFlowNodeId(
      ast().parents(node.id())
        .mapToObj(ast()::node)
        .filter(it -> it.data() instanceof Statement.Try || it.data() instanceof Declaration.Child)
        .findFirst()
        .orElseThrow()
        .id()
    );
  }

  @Override
  protected void statementTry(Node<Statement.Try> node) {
    int cf = visitControlFlowNodeId(node.data().block(ast()));
    var catches = new ArrayList<Node<Statement.Try.Catch>>();
    var iter = node.data().catches(ast()).iterator();
    while (iter.hasNext()) {
      var v = iter.next();
      int cfCatch = visitControlFlowNodeId(v.data().block(ast()));
      if (cf > 0 && cfCatch > 0)
        cf = closestParent(cf, cfCatch);
      else
        cf = 0;

      ast().typeOf(v.data().definition(ast())).ifPresent(typeCatch ->
        catches.stream()
          .filter(it ->
            Caster.runSimple(ast(), typeCatch, ast().typeOf(it.data().definition(ast())).orElseThrow())
          ).findFirst()
          .ifPresentOrElse(
            it -> diagnosticStatementTryCatchUnreachable(v, it),
            () -> catches.add(v)
          )
      );
    }
    if (node.data().blockFinallyId() > 0) {
      int cfFinally = visitControlFlowNodeId(node.data().blockFinally(ast()).orElseThrow());
      if (cfFinally == 0) setControlFlowNodeId(cf);
      else if (cf == 0) setControlFlowNodeId(cfFinally);
      else setControlFlowNodeId(closestParent(cf, cfFinally));
    } else {
      setControlFlowNodeId(cf);
    }
  }

  @Override
  protected void statementTryCatch(Node<Statement.Try.Catch> node) {
    visit(node.data().definitionId());
    setControlFlowNodeId(visitControlFlowNodeId(node.data().block(ast())));
  }

  @Override
  protected void statementCondition(Node<Statement.Condition> node) {
    visit(node.data().conditionId());
    int cfThen = visitControlFlowNodeId(node.data().then(ast()));
    if (node.data().otherId() > 0) {
      int cfOther = visitControlFlowNodeId(node.data().other(ast()).orElseThrow());
      if (cfThen == 0 || cfOther == 0)
        setControlFlowNodeId(0);
      else
        setControlFlowNodeId(closestParent(cfThen, cfOther));
    } else {
      setControlFlowNodeId(0);
    }
  }

  @Override
  protected void statementLoopDo(Node<Statement.LoopDo> node) {
    int cf = visitControlFlowNodeId(node.data().statement(ast()));
    visit(node.data().conditionId());

    setControlFlowNodeId(cf == node.id() ? 0 : cf);
  }

  @Override
  protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
    visit(node.data().conditionId());
    int cf = visitControlFlowNodeId(node.data().statement(ast()));

    setControlFlowNodeId(cf == node.id() ? 0 : cf);
  }

  @Override
  protected void statementLoopFor(Node<Statement.LoopFor> node) {
    if (node.data().initializer(ast()).orElse(null) instanceof Node(int id, Statement v))
      visitControlFlowNodeId(new Node<>(id, v));
    else if (node.data().initializerId() > 0)
      visit(node.data().initializerId());
    visit(node.data().conditionId());
    visit(node.data().updateId());
    int cf = visitControlFlowNodeId(node.data().statement(ast()));

    setControlFlowNodeId(cf == node.id() ? 0 : cf);
  }

  @Override
  protected void statementLoopForEach(Node<Statement.LoopForEach> node) {
    visit(node.data().definitionId());
    visit(node.data().expressionId());
    int cf = visitControlFlowNodeId(node.data().statement(ast()));

    setControlFlowNodeId(cf == node.id() ? 0 : cf);
  }

  @Override
  protected void expressionObject(Node<Expression.Object> node) {
    switch (ast().parent(node.id())) {
      case Node(int id, Statement.Expression _) when
        ast().parent(id) instanceof Node(int _, Declaration.Field _) -> {
      }
      case Node(int _, Statement.Variable _) -> {
      }
      case Node(int id, Expression.Binary _) when
        ast().parent(id) instanceof Node(int id2, Statement.Expression _) &&
          ast().parent(id2) instanceof Node(int _, Declaration.Initialize _) -> {
      }
      default -> diagnosticExpressionObject(node);
    }

    super.expressionObject(node);
  }

  private abstract class CheckOverride<T> extends Protector<T> {
    protected abstract void success(T result);

    @Override
    protected final Ast ast() {
      return Visitor3.this.ast();
    }

    protected final void absent() {
    }

    @Override
    protected final void success(Stream<? extends T> result) {
      result.filter(it -> (Declaration.STATIC & modifiers(it)) == 0)
        .findFirst()
        .ifPresentOrElse(this::success, this::absent);
    }

    protected final void run() {
      ast().inheritance(declaration())
        .skip(1)
        .findFirst()
        .ifPresent(this::run);
    }
  }
}
