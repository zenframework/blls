package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.ast.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Gatherer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.StreamSupport.stream;

public final class Ast extends org.zenframework.z8.blls.ast.Ast.Delegating {
  public Ast(org.zenframework.z8.blls.ast.Ast ast) {
    super(ast);
  }

  @SuppressWarnings("unchecked")
  public static <D extends Node.Data> Gatherer<Node<? extends Node.Data>, ?, Node<D>> only(Class<D> klass) {
    return Gatherer.ofSequential(Gatherer.Integrator.ofGreedy((_, it, downstream) -> {
      if (klass.isInstance(it.data()))
        downstream.push((Node<D>) it);
      return true;
    }));
  }

  public static <D extends Node.Data> Gatherer<Node<D>, ?, Duplicate> duplicatesInt(ToIntFunction<D> keyOf) {
    return Gatherer.ofSequential(MapInt<Node<D>>::new, Gatherer.Integrator.ofGreedy((map, it, downstream) -> {
      var oldValue = map.put(keyOf.applyAsInt(it.data()), it);
      if (oldValue != null) downstream.push(new Duplicate(oldValue.id(), it.id()));
      return true;
    }));
  }

  public static <D extends Node.Data, T> Gatherer<Node<D>, ?, Duplicate> duplicates(Function<Node<D>, T> keyOf) {
    return Gatherer.ofSequential(() -> new HashMap<T, Node<D>>(), Gatherer.Integrator.ofGreedy((map, it, downstream) -> {
      var oldValue = map.put(keyOf.apply(it), it);
      if (oldValue != null) downstream.push(new Duplicate(oldValue.id(), it.id()));
      return true;
    }));
  }

  public static <T> Collector<T, ?, Optional<T>> single() {
    class AccumulatorSingle {
      @Nullable T value = null;
      boolean multiple = false;

      void set(T value) {
        if (!multiple && this.value == null) {
          this.value = value;
        } else {
          this.value = null;
          multiple = true;
        }
      }
    }

    return Collector.of(
      AccumulatorSingle::new,
      AccumulatorSingle::set,
      (_, _) -> new AccumulatorSingle(),
      a -> Optional.ofNullable(a.value),
      Collector.Characteristics.UNORDERED
    );
  }

  public Stream<Node<org.zenframework.z8.blls.ast.Declaration>> nesting(Node<org.zenframework.z8.blls.ast.Declaration> node) {
    return parents(node.id())
      .mapToObj(this::node)
      .gather(only(org.zenframework.z8.blls.ast.Declaration.class));
  }

  Expression.@Nullable Type extendedOf(Node<org.zenframework.z8.blls.ast.Declaration> node) {
    return switch (parent(node.id())) {
      case Node(int _, org.zenframework.z8.blls.ast.Klass v) -> v.extended(this)
        .flatMap(this::typeOf)
        .orElse(null);
      case Node(int id, Expression.Object _) -> switch (parent(id)) {
        case Node(int id2, Statement.Expression _) when
          parent(id2) instanceof Node(int _, org.zenframework.z8.blls.ast.Declaration.Field v) ->
          typeOf(v.definition(this)).orElse(null);
        case Node(int _, Statement.Variable v) -> typeOf(v.definition(this)).orElse(null);
        case Node(int id2, Expression.Binary v) when
          parent(id2) instanceof Node(int id3, Statement.Expression _) &&
            parent(id3) instanceof Node(int _, org.zenframework.z8.blls.ast.Declaration.Initialize _) ->
          type(v.left(this)).orElse(null);
        default -> null;
      };
      default -> throw new AssertionError();
    };
  }

  public Stream<Node<org.zenframework.z8.blls.ast.Declaration>> inheritance(Node<org.zenframework.z8.blls.ast.Declaration> node) {
    return stream(new Spliterator<>() {
      private final MapInt<?>.Keys dejavu = new MapInt<>().keySet();
      private int id = node.id();

      @Override
      public boolean tryAdvance(Consumer<? super Node<org.zenframework.z8.blls.ast.Declaration>> action) {
        if (id == 0 || !dejavu.add(id)) {
          id = 0;
          return false;
        } else {
          Node<org.zenframework.z8.blls.ast.Declaration> n = node(id);
          action.accept(n);
          id = Ast.this.extendedOf(n) instanceof Expression.Type.Declaration(int id2) ? id2 : 0;
          return true;
        }
      }

      @Override
      public @Nullable Spliterator<Node<org.zenframework.z8.blls.ast.Declaration>> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
        return ORDERED | DISTINCT | NONNULL | IMMUTABLE;
      }
    }, false);
  }

  public Stream<Node<Klass>> inheritanceKlasses(Node<org.zenframework.z8.blls.ast.Declaration> node) {
    return inheritance(node).mapMulti((it, downstream) -> {
      if (parent(it.id()) instanceof Node(int id, Klass v)) {
        downstream.accept(new Node<>(id, v));
      }
    });
  }

  public <D extends org.zenframework.z8.blls.ast.Declaration.Child> Stream<Node<D>> overrides(Node<D> node) {
    return stream(new Spliterator<>() {
      private int id = node.id();

      @Override
      public boolean tryAdvance(Consumer<? super Node<D>> action) {
        if (id == 0) return false;

        Node<D> n = node(id);
        action.accept(n);

        id = switch (n.data()) {
          case org.zenframework.z8.blls.ast.Declaration.Method _ -> extra(id, Extra.DeclarationMethod.class).overrideId;
          case org.zenframework.z8.blls.ast.Declaration.Priority _ ->
            extra(id, Extra.DeclarationPriority.class).overrideId;
          case org.zenframework.z8.blls.ast.Declaration.Cast _ -> extra(id, Extra.DeclarationCast.class).overrideId;
          case org.zenframework.z8.blls.ast.Declaration.Operator _ ->
            extra(id, Extra.DeclarationOperator.class).overrideId;
          default -> throw new AssertionError();
        };
        return true;
      }

      @Override
      public @Nullable Spliterator<Node<D>> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
        return ORDERED | DISTINCT | NONNULL | IMMUTABLE;
      }
    }, false);
  }

  public Optional<Expression.Type> typeOf(Node<? extends Definition> node) {
    return switch (node.data()) {
      case Definition.Reference _ ->
        switch (extra(node.id(), Extra.DefinitionReference.class).getResolved(this).orElse(null)) {
          case null -> Optional.empty();
          case Ast.Pack _ -> Optional.empty();
          case Ast.Declaration(int v) -> Optional.of(new Expression.Type.Declaration(v));
          case Ast.Enumeration(int v) -> Optional.of(new Expression.Type.Enumeration(v));
        };
      case Definition.Array v -> typeOf(v.value(this)).map(Expression.Type.Array::new);
      case Definition.Map v -> {
        var key = typeOf(v.key(this));
        if (key.isEmpty()) yield Optional.empty();

        var value = typeOf(v.value(this));
        if (value.isEmpty()) yield Optional.empty();

        yield Optional.of(new Expression.Type.Map(key.get(), value.get()));
      }
    };
  }

  public Map<String, Expression.Type.Declaration> primaries() {
    return Stream.concat(
        Stream.of(
          "void",
          "primary",
          "binary",
          "bool",
          "date",
          "datespan",
          "decimal",
          "exception",
          "file",
          "geometry",
          "guid",
          "int",
          "string"
        ).map(it -> List.of("org", "zenframework", "z8", "lang", it)),
        Stream.of(
          "sql_binary",
          "sql_bool",
          "sql_date",
          "sql_datespan",
          "sql_decimal",
          "sql_geometry",
          "sql_guid",
          "sql_int",
          "sql_string"
        ).map(it -> List.of("org", "zenframework", "z8", "lang", "sql", it))
      )
      .flatMap(it -> Resolver.runSimple(this, it.iterator()).stream())
      .gather(only(org.zenframework.z8.blls.ast.Declaration.class))
      .collect(toUnmodifiableMap(
        it -> this.<Klass>parent(it.id()).data().name(this),
        it -> new Expression.Type.Declaration(it.id())
      ));
  }

  public boolean isReference(Expression.Type type) {
    return type instanceof Expression.Type.Declaration v && !isPrimary(v.node(this));
  }

  // TODO: точнее скопировать VariableType::isArrayOfReferences
  public boolean isArrayOfReferences(Expression.Type type) {
    return type instanceof Expression.Type.Array(var child) &&
      child instanceof Expression.Type.Declaration v &&
      !isPrimary(v.node(this));
  }

  public boolean isPrimary(Node<org.zenframework.z8.blls.ast.Declaration> node) {
    return inheritanceKlasses(node)
      .findFirst()
      .filter(it -> annotationOf(it, Annotation.PRIMARY).isPresent())
      .isPresent();
  }

  public boolean isNative(Node<org.zenframework.z8.blls.ast.Declaration> node) {
    return isPrimary(node) || inheritanceKlasses(node)
      .findFirst()
      .filter(it -> annotationOf(it, Annotation.NATIVE).isPresent())
      .isPresent();
  }

  public Optional<Node<Annotation>> annotationOf(Node<Klass> node, String name) {
    return node.data()
      .annotations(this)
      .filter(it -> it.data().name(this).equals(name))
      .findFirst();
  }

  public Node<org.zenframework.z8.blls.ast.Declaration> declarationOf(Node<?> node) {
    return parents(node.id())
      .mapToObj(this::node)
      .gather(only(org.zenframework.z8.blls.ast.Declaration.class))
      .findFirst()
      .orElseThrow();
  }

  public Stream<Node<Expression.Access>> initializeAccesses(org.zenframework.z8.blls.ast.Declaration.Initialize data) {
    return stream(new Spliterator<>() {
      int id = switch (data.child(Ast.this).data().value(Ast.this)) {
        case Node(int id2, Expression.Access _) -> id2;
        case Node(int _, Expression.Binary v) -> v.leftId();
        default -> throw new AssertionError();
      };

      @Override
      public boolean tryAdvance(Consumer<? super Node<Expression.Access>> action) {
        if (id == 0) return false;

        Node<Expression.Access> node = node(id);
        action.accept(node);
        id = node.data().childId();

        return true;
      }

      @Override
      public @Nullable Spliterator<Node<Expression.Access>> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
        return ORDERED | DISTINCT | NONNULL | IMMUTABLE;
      }
    }, false);
  }

  public Optional<Node<org.zenframework.z8.blls.ast.Declaration.Field>> fieldOf(org.zenframework.z8.blls.ast.Declaration.Initialize data) {
    return initializeAccesses(data)
      .findFirst()
      .flatMap(it -> extra(it, Extra.ExpressionAccess.class).getResolved(this))
      .orElse(null)
      instanceof Extra.ExpressionAccess.Resolved.DeclarationField(int id)
      ? Optional.of(node(id))
      : Optional.empty();
  }

  public Optional<Object> annotationConstant(Node<Annotation> node) {
    return switch (node.data().value(this).map(Node::data).orElse(null)) {
      case null -> Optional.of(new Expression.Type.Null());
      case Expression.Boolean v -> Optional.of(v.value());
      case Expression.Integer v -> Optional.of(v.value());
      case Expression.String v -> Optional.of(v.value(this));
      default -> Optional.empty();
    };
  }

  public Stream<Node<Annotation>> annotationsOf(
    Node<org.zenframework.z8.blls.ast.Declaration> declaration,
    Node<org.zenframework.z8.blls.ast.Declaration.Field> node
  ) {
    return Stream.concat(
      // аннотации из родительских классов, определённые в Declaration.Initialize для этого же поля
      inheritance(declaration).flatMap(parentDeclaration ->
        parentDeclaration.data()
          .children(this)
          .gather(Ast.only(org.zenframework.z8.blls.ast.Declaration.Initialize.class))
          .filter(it -> node.equals(fieldOf(it.data()).orElse(null)))
          .flatMap(it -> it.data().annotations(this))
      ),
      // аннотации на самом поле
      node.data().annotations(this)
    );
  }

  public <D extends Node.Data, E extends Ast.Extra<D>> E extra(Node<D> node, Class<E> klass) {
    return extra(node.id(), klass);
  }

  public <D extends Node.Data, E extends Ast.Extra<D>> void extraUpdate(Node<D> node, Class<E> klass, Consumer<E> action) {
    E e = extra(node, klass);
    action.accept(e);
    extraSet(node.id(), e);
  }

  public sealed interface Extra<D extends Node.Data> {
    final class Source implements Extra<org.zenframework.z8.blls.ast.Source> {
      public int packId;

      public Ast.Pack pack() {
        return new Ast.Pack(packId);
      }
    }

    final class Import implements Extra<org.zenframework.z8.blls.ast.Import> {
      public int resolvedId;
      public boolean used;
    }

    final class Enumeration implements Extra<org.zenframework.z8.blls.ast.Enumeration> {
      public int packId;
    }

    final class Declaration implements Extra<org.zenframework.z8.blls.ast.Declaration> {
      public int packId;
    }

    final class DeclarationField implements Extra<org.zenframework.z8.blls.ast.Declaration.Field> {
      public int valueCastId;
      public int resolvedAssignId;

      public Optional<Node<org.zenframework.z8.blls.ast.Declaration.Operator>> resolvedAssign(org.zenframework.z8.blls.ast.Ast ast) {
        return resolvedAssignId == 0 ? Optional.empty() : Optional.of(ast.node(resolvedAssignId));
      }
    }

    final class DeclarationMethod implements Extra<org.zenframework.z8.blls.ast.Declaration.Method> {
      public int overrideId;

      public record Key(String name, List<Expression.Type> types) {
        public static Optional<Key> of(Ast ast, Node<org.zenframework.z8.blls.ast.Declaration.Method> node) {
          List<Expression.@Nullable Type> types = node.data().parameters(ast)
            .map(it -> ast.typeOf(it.data().definition(ast)).orElse(null))
            .toList();
          if (types.stream().anyMatch(Objects::isNull))
            return Optional.empty();

          return Optional.of(new Extra.DeclarationMethod.Key(node.data().name(ast), types));
        }

        public boolean equalsSignature(Key other) {
          return name().equals(other.name()) && IntStream.range(0, types().size())
            .allMatch(i -> types().get(i).getClass().equals(other.types().get(i).getClass()));
        }
      }
    }

    final class DeclarationPriority implements Extra<org.zenframework.z8.blls.ast.Declaration.Priority> {
      public int overrideId;
    }

    final class DeclarationCast implements Extra<org.zenframework.z8.blls.ast.Declaration.Cast> {
      public int overrideId;
    }

    final class DeclarationOperator implements Extra<org.zenframework.z8.blls.ast.Declaration.Operator> {
      public int overrideId;

      public record Key(Operation operation, Expression.@Nullable Type type) {
        public static Optional<Key> of(Ast ast, Node<org.zenframework.z8.blls.ast.Declaration.Operator> node) {
          return switch (node.data().operation()) {
            case Operation.ADD,
                 Operation.SUBTRACT -> {
              var iter = node.data().parameters(ast).iterator();
              if (!iter.hasNext())
                yield Optional.of(new Key(node.data().operation(), null));

              var p = iter.next();
              if (iter.hasNext())
                yield Optional.empty();

              yield ast.typeOf(p.data().definition(ast))
                .map(it -> new Key(node.data().operation(), it));
            }
            case Operation.MULTIPLY,
                 Operation.DIVIDE,
                 Operation.MODULE,
                 Operation.LESS,
                 Operation.LESS_EQUAL,
                 Operation.MORE,
                 Operation.MORE_EQUAL,
                 Operation.EQUAL,
                 Operation.NOT_EQUAL,
                 Operation.AND,
                 Operation.OR,
                 Operation.BIT_AND,
                 Operation.BIT_OR,
                 Operation.BIT_XOR,
                 Operation.ASSIGN -> node.data().parameters(ast)
              .collect(single())
              .flatMap(it -> ast.typeOf(it.data().definition(ast)))
              .map(it -> new Key(node.data().operation(), it));
            case Operation.NOT,
                 Operation.BIT_NOT -> node.data().parametersId() == 0
              ? Optional.of(new Key(Operation.NOT, null))
              : Optional.empty();
            default -> throw new AssertionError();
          };
        }

        public boolean equalsSignature(Key other) {
          return operation() == other.operation() && (
            (type() == null && other.type() == null) ||
              (type() != null && other.type() != null && type().getClass().equals(other.type().getClass()))
          );
        }
      }
    }

    final class StatementVariable implements Extra<Statement.Variable> {
      public int valueCastId;
      public int resolvedAssignId;

      public Optional<Node<org.zenframework.z8.blls.ast.Declaration.Operator>> resolvedAssign(org.zenframework.z8.blls.ast.Ast ast) {
        return resolvedAssignId == 0 ? Optional.empty() : Optional.of(ast.node(resolvedAssignId));
      }
    }

    final class StatementReturn implements Extra<Statement.Return> {
      public int valueCastId;
      public int resolvedPriorityId;

      public Optional<Node<org.zenframework.z8.blls.ast.Declaration.Priority>> resolvedPriority(org.zenframework.z8.blls.ast.Ast ast) {
        return resolvedPriorityId == 0 ? Optional.empty() : Optional.of(ast.node(resolvedPriorityId));
      }
    }

    final class StatementThrow implements Extra<Statement.Throw> {
      public int valueCastId;
    }

    final class StatementCondition implements Extra<Statement.Condition> {
      public int conditionCastId;
    }

    final class StatementLoopDo implements Extra<Statement.LoopDo> {
      public int conditionCastId;
    }

    final class StatementLoopWhile implements Extra<Statement.LoopWhile> {
      public int conditionCastId;
    }

    final class StatementLoopFor implements Extra<Statement.LoopFor> {
      public int conditionCastId;
    }

    final class ExpressionParen implements Extra<Expression.Paren> {
      public int resolvedPriorityId;

      public Optional<Node<org.zenframework.z8.blls.ast.Declaration.Priority>> resolvedPriority(org.zenframework.z8.blls.ast.Ast ast) {
        return resolvedPriorityId == 0 ? Optional.empty() : Optional.of(ast.node(resolvedPriorityId));
      }
    }

    final class ExpressionAccess implements Extra<Expression.Access> {
      public int resolvedId;

      public Optional<Resolved> getResolved(org.zenframework.z8.blls.ast.Ast ast) {
        if (resolvedId == 0)
          return Optional.empty();
        else if (resolvedId < 0)
          return Optional.of(new Resolved.Pack(resolvedId));
        else
          return Optional.of(switch (ast.data(resolvedId)) {
            case org.zenframework.z8.blls.ast.Declaration _ -> new Resolved.Declaration(resolvedId);
            case org.zenframework.z8.blls.ast.Enumeration _ -> new Resolved.Enumeration(resolvedId);
            case org.zenframework.z8.blls.ast.Declaration.Field _ -> new Resolved.DeclarationField(resolvedId);
            case org.zenframework.z8.blls.ast.Declaration.Records.Child _ ->
              new Resolved.DeclarationRecordsChild(resolvedId);
            case org.zenframework.z8.blls.ast.Enumeration.Child _ -> new Resolved.EnumerationChild(resolvedId);
            case Parameter _ -> new Resolved.Parameter(resolvedId);
            case Statement.Variable _ -> new Resolved.StatementVariable(resolvedId);
            case Statement.LoopForEach _ -> new Resolved.StatementLoopForEach(resolvedId);
            case Statement.Try.Catch _ -> new Resolved.StatementTryCatch(resolvedId);
            default -> throw new AssertionError();
          });
      }

      public void setResolved(Ast.PackChild value) {
        resolvedId = value.id();
      }

      public void setResolvedDeclarationField(Node<org.zenframework.z8.blls.ast.Declaration.Field> node) {
        resolvedId = node.id();
      }

      public void setResolvedDeclarationRecordsChild(Node<org.zenframework.z8.blls.ast.Declaration.Records.Child> node) {
        resolvedId = node.id();
      }

      public void setResolvedEnumerationChild(Node<org.zenframework.z8.blls.ast.Enumeration.Child> node) {
        resolvedId = node.id();
      }

      public sealed interface Resolved {
        int id();

        record Pack(int id) implements Resolved {
          public Ast.Pack pack() {
            return new Ast.Pack(id());
          }
        }

        record Declaration(int id) implements Resolved {
          public Node<org.zenframework.z8.blls.ast.Declaration> node(org.zenframework.z8.blls.ast.Ast ast) {
            return ast.node(id());
          }
        }

        record Enumeration(int id) implements Resolved {
          public Node<org.zenframework.z8.blls.ast.Enumeration> node(org.zenframework.z8.blls.ast.Ast ast) {
            return ast.node(id());
          }
        }

        record DeclarationField(int id) implements Resolved {
        }

        record DeclarationRecordsChild(int id) implements Resolved {
        }

        record EnumerationChild(int id) implements Resolved {
        }

        record Parameter(int id) implements Resolved {
        }

        record StatementVariable(int id) implements Resolved {
        }

        record StatementLoopForEach(int id) implements Resolved {
        }

        record StatementTryCatch(int id) implements Resolved {
        }
      }
    }

    final class ExpressionIndex implements Extra<Expression.Index> {
      public int valueCastId;
    }

    final class ExpressionInvoke implements Extra<Expression.Invoke> {
      public int resolvedId;
      public Synthetic.@Nullable Method resolvedSynthetic;
      public boolean argumentsInvalid;

      public Optional<Resolved> getResolved(org.zenframework.z8.blls.ast.Ast ast) {
        if (resolvedId == 0)
          return Optional.empty();
        else
          return Optional.of(switch (ast.data(resolvedId)) {
            case org.zenframework.z8.blls.ast.Declaration.Method _ -> new Resolved.DeclarationMethod(resolvedId);
            case org.zenframework.z8.blls.ast.Declaration.Cast _ -> new Resolved.DeclarationCast(resolvedId);
            default -> throw new AssertionError();
          });
      }

      public void setResolvedDeclarationMethod(Node<org.zenframework.z8.blls.ast.Declaration.Method> node) {
        resolvedId = node.id();
      }

      public void setResolvedDeclarationCast(Node<org.zenframework.z8.blls.ast.Declaration.Cast> node) {
        resolvedId = node.id();
      }

      public sealed interface Resolved {
        int id();

        record DeclarationMethod(int id) implements Resolved {
          public Node<org.zenframework.z8.blls.ast.Declaration.Method> node(org.zenframework.z8.blls.ast.Ast ast) {
            return ast.node(id());
          }
        }

        record DeclarationCast(int id) implements Resolved {
          public Node<org.zenframework.z8.blls.ast.Declaration.Cast> node(org.zenframework.z8.blls.ast.Ast ast) {
            return ast.node(id());
          }
        }
      }
    }

    final class ExpressionCast implements Extra<Expression.Cast> {
      public int valueCastId;
    }

    final class ExpressionUnary implements Extra<Expression.Unary> {
      public int resolvedId;
      public int valueCastId;

      public Optional<Node<org.zenframework.z8.blls.ast.Declaration.Operator>> resolved(org.zenframework.z8.blls.ast.Ast ast) {
        return resolvedId == 0 ? Optional.empty() : Optional.of(ast.node(resolvedId));
      }
    }

    final class ExpressionBinary implements Extra<Expression.Binary> {
      public int resolvedId;
      public Synthetic.@Nullable Operator resolvedSynthetic;
      public int leftCastId;
      public int rightCastId;
      public int resolvedAssignId;
      public int rightCastAssignId;

      public Optional<Node<org.zenframework.z8.blls.ast.Declaration.Operator>> resolved(org.zenframework.z8.blls.ast.Ast ast) {
        return resolvedId == 0 ? Optional.empty() : Optional.of(ast.node(resolvedId));
      }

      public Optional<Node<org.zenframework.z8.blls.ast.Declaration.Operator>> resolvedAssign(org.zenframework.z8.blls.ast.Ast ast) {
        return resolvedAssignId == 0 ? Optional.empty() : Optional.of(ast.node(resolvedAssignId));
      }
    }

    final class ExpressionTernary implements Extra<Expression.Ternary> {
      public int conditionCastId;
      public int thenCastId;
      public int otherCastId;
    }

    final class ExpressionArrayChildValue implements Extra<Expression.Array.Child.Value> {
      public int valueCastId;
    }

    final class ExpressionArrayChildEntry implements Extra<Expression.Array.Child.Entry> {
      public int keyCastId;
      public int valueCastId;
    }

    final class ExpressionInvokeArgument implements Extra<Expression.Invoke.Argument> {
      public int valueCastId;
    }

    final class DefinitionReference implements Extra<Definition.Reference> {
      public int resolvedId;

      public Optional<? extends Ast.PackChild> getResolved(org.zenframework.z8.blls.ast.Ast ast) {
        if (resolvedId == 0)
          return Optional.empty();
        else if (resolvedId < 0)
          return Optional.of(new Ast.Pack(resolvedId));
        else
          return Optional.of(switch (ast.data(resolvedId)) {
            case org.zenframework.z8.blls.ast.Declaration _ -> new Ast.Declaration(resolvedId);
            case org.zenframework.z8.blls.ast.Enumeration _ -> new Ast.Enumeration(resolvedId);
            default -> throw new AssertionError();
          });
      }

      public void setResolved(Ast.PackChild value) {
        resolvedId = value.id();
      }
    }
  }

  public record Duplicate(int oldId, int newId) {
  }
}
