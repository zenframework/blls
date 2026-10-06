package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.Declaration;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Node;
import org.zenframework.z8.blls.ast.Operation;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/// Класс отвечает за доступ к элементам типов
public abstract class Accessor<T> extends Protector<T> {
  protected abstract Object key(T value);

  protected abstract void absent();

  protected abstract void run(Expression.Type.Enumeration source);

  protected abstract void run(Expression.Type.Array source);

  protected abstract void run(Expression.Type.Map source);

  /// Доступ осуществлён к элементам объявления
  ///
  /// В отличие от success() не содержит переопределённых методов
  protected abstract void success2(Stream<? extends T> result);

  @Override
  protected final void success(Stream<? extends T> result) {
    // Исключает выдачу переопределённых методов в качестве возможных для доступа
    success2(result.collect(toMap(this::key, Function.identity(), (oldValue, _) -> oldValue)).values().stream());
  }

  public final void run(Expression.Type source) {
    switch (source) {
      case Expression.Type.Null _ -> absent();
      case Expression.Type.Declaration(int id) -> run(ast().node(id));
      case Expression.Type.Enumeration v -> run(v);
      case Expression.Type.Array v -> run(v);
      case Expression.Type.Map v -> run(v);
    }
  }

  /// Класс отвечает за доступ к полям и записям
  public abstract static class FieldOrRecord extends Accessor<FieldOrRecord.Proxy> {
    protected abstract int nameId();

    protected abstract void success(FieldOrRecord.Proxy result);

    @Override
    protected final int modifiers(Proxy value) {
      return switch (value) {
        case ProxyField v -> v.node().data().modifiers();
        case ProxyRecordsChild v -> v.node().data().modifiers();
      };
    }

    @Override
    protected final Stream<FieldOrRecord.Proxy> search(Node<Declaration> node) {
      return node.data().children(ast())
        .gather(Ast.only(Declaration.Field.class))
        .filter(it -> it.data().nameId() == nameId())
        .<FieldOrRecord.Proxy>map(FieldOrRecord.ProxyField::new)
        .findFirst()
        .or(() ->
          node.data().children(ast())
            .gather(Ast.only(Declaration.Records.class))
            .flatMap(it -> it.data().children(ast()))
            .filter(it -> it.data().nameId() == nameId())
            .map(FieldOrRecord.ProxyRecordsChild::new)
            .findFirst()
        ).stream();
    }

    @Override
    protected final void success2(Stream<? extends FieldOrRecord.Proxy> result) {
      result.findFirst().ifPresentOrElse(this::success, this::absent);
    }

    @Override
    protected final Object key(Proxy value) {
      return switch (value) {
        case ProxyField v -> v.node().data().name(ast());
        case ProxyRecordsChild v -> v.node().data().name(ast());
      };
    }

    @Override
    protected final void run(Expression.Type.Enumeration source) {
      absent();
    }

    @Override
    protected final void run(Expression.Type.Array source) {
      absent();
    }

    @Override
    protected final void run(Expression.Type.Map source) {
      absent();
    }

    protected sealed interface Proxy {
    }

    protected record ProxyField(Node<Declaration.Field> node) implements Proxy {
    }

    protected record ProxyRecordsChild(Node<Declaration.Records.Child> node) implements Proxy {
    }
  }

  /// Класс отвечает за доступ к методам
  ///
  /// Также обрабатывается доступ к встроенным методам массивов и словарей
  public abstract static class Method extends Accessor<Method.Proxy> {
    protected abstract Optional<Expression.Type.Declaration> pritimiveOf(String name);

    protected abstract int nameId();

    protected abstract List<Node<Expression.Invoke.Argument>> arguments();

    protected abstract void setCast(int index, Node<Declaration.Cast> node);

    protected abstract void ambiguous(Stream<? extends Method.Proxy> result);

    protected abstract void successArgumentsInvalid(Method.Proxy result);

    protected abstract void success(Method.Proxy result);

    @Override
    protected final int modifiers(Proxy value) {
      return switch (value) {
        case ProxySynthetic _ -> Declaration.PUBLIC | Declaration.FINAL;
        case ProxyMethod v -> v.node().data().modifiers();
        case ProxyCast v -> v.node().data().modifiers();
      };
    }

    @Override
    protected final Stream<? extends Method.Proxy> search(Node<Declaration> node) {
      var result = node.data().children(ast())
        .gather(Ast.only(Declaration.Method.class))
        .filter(it -> it.data().nameId() == nameId())
        // Сразу отбросить методы, не подходящие по количеству переданных аргументов
        .filter(it -> it.data().parameters(ast()).count() == arguments().size())
        .filter(it -> Extra.DeclarationMethod.Key.of(ast(), it).isPresent())
        .map(Method.ProxyMethod::new);

      if (arguments().isEmpty()) return Stream.concat(
        result,
        node.data().children(ast())
          .gather(Ast.only(Declaration.Cast.class))
          .filter(it -> it.data().definition(ast()).data().nameId() == nameId())
          .map(Accessor.Method.ProxyCast::new)
          .findFirst()
          .stream()
      );
      else return result;
    }

    @Override
    protected final Object key(Proxy value) {
      return switch (value) {
        case ProxySynthetic v -> new Extra.DeclarationMethod.Key(
          v.synthetic().name(),
          v.synthetic().parameters()
            .stream()
            .map(Synthetic.Parameter::type)
            .toList()
        );
        case ProxyMethod v -> Extra.DeclarationMethod.Key.of(ast(), v.node());
        case ProxyCast v -> v.node().data().definition(ast()).data().name(ast());
      };
    }

    @Override
    protected final void run(Expression.Type.Enumeration source) {
      // У перечислений в объектном контексте не может быть методов
      absent();
    }

    @Override
    protected final void run(Expression.Type.Array source) {
      var typeVoid = pritimiveOf("void").orElse(null);
      var typeInt = pritimiveOf("int").orElse(null);
      var typeBool = pritimiveOf("bool").orElse(null);
      var typeString = pritimiveOf("string").orElse(null);

      if (typeVoid == null || typeInt == null || typeBool == null || typeString == null) absent();
      else switch (ast().string(nameId())) {
        case "size" -> run(
          new Synthetic.Method(
            source,
            "size",
            List.of(),
            typeInt
          )
        );
        case "isEmpty" -> run(
          new Synthetic.Method(
            source,
            "isEmpty",
            List.of(),
            typeBool
          )
        );
        case "contains" -> run(
          new Synthetic.Method(
            source,
            "contains",
            List.of(new Synthetic.Parameter(source.value(), "element")),
            typeBool
          )
        );
        case "isSubsetOf" -> run(
          new Synthetic.Method(
            source,
            "isSubsetOf",
            List.of(new Synthetic.Parameter(source, "element")),
            typeBool
          )
        );
        case "indexOf" -> run(
          new Synthetic.Method(
            source,
            "indexOf",
            List.of(new Synthetic.Parameter(source.value(), "element")),
            typeInt
          )
        );
        case "clear" -> run(
          new Synthetic.Method(
            source,
            "clear",
            List.of(),
            typeVoid
          )
        );
        case "add" -> run(
          new Synthetic.Method(
            source,
            "add",
            List.of(new Synthetic.Parameter(source.value(), "element")),
            typeVoid
          ),
          new Synthetic.Method(
            source,
            "add",
            List.of(
              new Synthetic.Parameter(typeInt, "index"),
              new Synthetic.Parameter(source.value(), "element")
            ),
            typeVoid
          )
        );
        case "set" -> run(
          new Synthetic.Method(
            source,
            "set",
            List.of(
              new Synthetic.Parameter(typeInt, "index"),
              new Synthetic.Parameter(source.value(), "element")
            ),
            typeVoid
          )
        );
        case "addAll" -> run(
          new Synthetic.Method(
            source,
            "addAll",
            List.of(new Synthetic.Parameter(source, "elements")),
            typeVoid
          ),
          new Synthetic.Method(
            source,
            "addAll",
            List.of(
              new Synthetic.Parameter(typeInt, "index"),
              new Synthetic.Parameter(source, "elements")
            ),
            typeVoid
          )
        );
        case "removeAt" -> run(
          new Synthetic.Method(
            source,
            "removeAt",
            List.of(new Synthetic.Parameter(typeInt, "index")),
            source.value()
          )
        );
        case "remove" -> run(
          new Synthetic.Method(
            source,
            "remove",
            List.of(new Synthetic.Parameter(source.value(), "element")),
            typeBool
          )
        );
        case "removeAll" -> run(
          new Synthetic.Method(
            source,
            "removeAll",
            List.of(new Synthetic.Parameter(source.value(), "element")),
            source
          ),
          new Synthetic.Method(
            source,
            "removeAll",
            List.of(new Synthetic.Parameter(source, "elements")),
            source
          )
        );
        case "sort" -> run(
          new Synthetic.Method(
            source,
            "sort",
            List.of(),
            source
          )
        );
        case "unique" -> run(
          new Synthetic.Method(
            source,
            "unique",
            List.of(),
            source
          )
        );
        case "join" -> run(
          new Synthetic.Method(
            source,
            "join",
            List.of(),
            typeString
          ),
          new Synthetic.Method(
            source,
            "join",
            List.of(new Synthetic.Parameter(typeString, "separator")),
            typeString
          )
        );
        default -> absent();
      }
    }

    @Override
    protected final void run(Expression.Type.Map source) {
      var typeVoid = pritimiveOf("void").orElse(null);
      var typeInt = pritimiveOf("int").orElse(null);
      var typeBool = pritimiveOf("bool").orElse(null);
      var typeString = pritimiveOf("string").orElse(null);

      if (typeVoid == null || typeInt == null || typeBool == null || typeString == null) absent();
      else switch (ast().string(nameId())) {
        case "size" -> run(
          new Synthetic.Method(
            source,
            "size",
            List.of(),
            typeInt
          )
        );
        case "isEmpty" -> run(
          new Synthetic.Method(
            source,
            "isEmpty",
            List.of(),
            typeBool
          )
        );
        case "clear" -> run(
          new Synthetic.Method(
            source,
            "clear",
            List.of(),
            typeVoid
          )
        );
        case "add" -> run(
          new Synthetic.Method(
            source,
            "add",
            List.of(
              new Synthetic.Parameter(source.key(), "key"),
              new Synthetic.Parameter(source.value(), "value")
            ),
            typeVoid
          ),
          new Synthetic.Method(
            source,
            "add",
            List.of(new Synthetic.Parameter(source, "map")),
            typeVoid
          )
        );
        case "remove" -> run(
          new Synthetic.Method(
            source,
            "remove",
            List.of(new Synthetic.Parameter(source.key(), "key")),
            source.value()
          )
        );
        case "containsKey" -> run(
          new Synthetic.Method(
            source,
            "containsKey",
            List.of(new Synthetic.Parameter(source.key(), "key")),
            typeBool
          )
        );
        case "containsValue" -> run(
          new Synthetic.Method(
            source,
            "containsValue",
            List.of(new Synthetic.Parameter(source.value(), "value")),
            typeBool
          )
        );
        case "keys" -> run(
          new Synthetic.Method(
            source,
            "keys",
            List.of(),
            new Expression.Type.Array(source.key())
          )
        );
        case "values" -> run(
          new Synthetic.Method(
            source,
            "values",
            List.of(),
            new Expression.Type.Array(source.value())
          )
        );
        default -> absent();
      }
    }

    private void run(Synthetic.Method... values) {
      success2(
        Arrays.stream(values)
          .filter(it -> it.parameters().size() == arguments().size())
          .map(Method.ProxySynthetic::new)
      );
    }

    @Override
    protected final void success2(Stream<? extends Method.Proxy> result) {
      boolean argumentsValid = arguments()
        .stream()
        // Набор аргументов неправильный, если хотя бы 1 не разрешён или не завершён
        // В дальнейшем принимается, что все аргументы имеют полный тип
        .allMatch(it -> ast().type(it.data().value(ast())).isPresent());

      if (argumentsValid) {
        new CasterMulti<CasterMulti.SourceProxy, Method.Proxy>() {
          @Override
          protected Ast ast() {
            return Method.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return Method.this.declaration();
          }

          @Override
          protected Stream<Expression.Type> parameters(Proxy target) {
            return switch (target) {
              case Method.ProxySynthetic v -> v.synthetic()
                .parameters()
                .stream()
                .map(Synthetic.Parameter::type);
              case Method.ProxyMethod v -> v.node().data()
                .parameters(ast())
                .map(it -> ast().typeOf(it.data().definition(ast())).orElseThrow());
              case Method.ProxyCast v -> v.node().data()
                .parameters(ast())
                .map(it -> ast().typeOf(it.data().definition(ast())).orElseThrow());
            };
          }

          @Override
          protected void absent() {
            Method.this.absent();
          }

          @Override
          protected void ambiguous(Stream<Method.Proxy> result) {
            Method.this.ambiguous(result);
          }

          @Override
          protected void success(Method.Proxy result) {
            Method.this.success(result);
          }
        }.run(new Arguments(), result);
      } else {
        // Если на выбор есть только 1 метод - можно выбрать его,
        // несмотря на наличие неправильного(ых) аргумента(ов)
        result.collect(Ast.single()).ifPresent(this::successArgumentsInvalid);

        // Если методов много - забить
      }
    }

    protected sealed interface Proxy {
    }

    protected record ProxySynthetic(Synthetic.Method synthetic) implements Proxy {
    }

    protected record ProxyMethod(Node<Declaration.Method> node) implements Proxy {
    }

    protected record ProxyCast(Node<Declaration.Cast> node) implements Proxy {
    }

    private final class Arguments extends AbstractList<CasterMulti.SourceProxy> {
      @Override
      public Proxy get(int index) {
        return new Proxy(index);
      }

      @Override
      public int size() {
        return arguments().size();
      }

      private final class Proxy implements CasterMulti.SourceProxy {
        private final int index;

        private Proxy(int index) {
          this.index = index;
        }

        @Override
        public Expression.Type type() {
          return ast().type(arguments().get(index).data().value(ast())).orElseThrow();
        }

        @Override
        public void setCast(Node<Declaration.Cast> node) {
          Method.this.setCast(index, node);
        }
      }
    }
  }

  /// Класс отвечает за доступ к операторам преобразования
  public abstract static class Cast extends Accessor<Node<Declaration.Cast>> {
    @Override
    protected final int modifiers(Node<Declaration.Cast> value) {
      return value.data().modifiers();
    }

    @Override
    protected final Expression.Type key(Node<Declaration.Cast> value) {
      return ast().typeOf(value.data().definition(ast())).orElseThrow();
    }

    @Override
    protected final void run(Expression.Type.Enumeration source) {
      absent();
    }

    @Override
    protected final void run(Expression.Type.Array source) {
      absent();
    }

    @Override
    protected final void run(Expression.Type.Map source) {
      absent();
    }

    @Override
    protected final Stream<Node<Declaration.Cast>> search(Node<Declaration> node) {
      return node.data().children(ast())
        .gather(Ast.only(Declaration.Cast.class))
        .filter(it -> ast().typeOf(it.data().definition(ast())).isPresent());
    }
  }

  /// Класс отвечает за доступ к унарным/бинарным операторам
  ///
  /// Также обрабатывается доступ к встроенным операторам массивов и словарей
  public abstract static class Operator<T> extends Accessor<T> {
    protected abstract Expression.Type target();

    protected abstract Operation operation();

    protected abstract void ambiguous(Stream<? extends T> result);

    protected abstract void success(T result);

    /// Класс отвечает за доступ к оператору присвоения
    public abstract static class Assign extends Operator<Node<Declaration.Operator>> implements CasterMulti.SourceProxy {
      protected abstract void failedAssign();

      protected abstract void success();

      @Override
      protected final Operation operation() {
        return Operation.ASSIGN;
      }

      @Override
      protected final void absent() {
        // Если operator= не переопределён - используется простое присваивание
        // (при этом возможно использование оператора преобразования)
        new CasterEnhanced() {
          @Override
          protected Ast ast() {
            return Assign.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return Assign.this.declaration();
          }

          @Override
          protected void failed() {
            Assign.this.failedAssign();
          }

          @Override
          protected void success(int distance) {
            Assign.this.success();
          }

          @Override
          protected void success() {
            Assign.this.success();
          }

          @Override
          protected void success(Node<Declaration.Cast> node) {
            Assign.this.setCast(node);
            Assign.this.success();
          }
        }.run(type(), target());
      }

      @Override
      protected final int modifiers(Node<Declaration.Operator> value) {
        return value.data().modifiers();
      }

      @Override
      protected final Extra.DeclarationOperator.Key key(Node<Declaration.Operator> value) {
        return Extra.DeclarationOperator.Key.of(ast(), value).orElseThrow();
      }

      @Override
      protected final Stream<Node<Declaration.Operator>> search(Node<Declaration> node) {
        return node.data().children(ast())
          .gather(Ast.only(Declaration.Operator.class))
          .filter(it -> it.data().operation() == operation())
          .filter(it -> Extra.DeclarationOperator.Key.of(ast(), it).isPresent());
      }

      @Override
      protected final void success2(Stream<? extends Node<Declaration.Operator>> result) {
        new CasterMulti<CasterMulti.SourceProxy, Node<Declaration.Operator>>() {
          @Override
          protected Ast ast() {
            return Assign.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return Assign.this.declaration();
          }

          @Override
          protected Stream<Expression.Type> parameters(Node<Declaration.Operator> target) {
            return target.data()
              .parameters(ast())
              .map(it -> ast().typeOf(it.data().definition(ast())).orElseThrow());
          }

          @Override
          protected void absent() {
            Assign.this.absent();
          }

          @Override
          protected void ambiguous(Stream<Node<Declaration.Operator>> result) {
            Assign.this.ambiguous(result);
          }

          @Override
          protected void success(Node<Declaration.Operator> result) {
            Assign.this.success(result);
          }
        }.run(List.of(this), result);
      }

      @Override
      protected final void run(Expression.Type.Enumeration source) {
        absent();
      }

      @Override
      protected final void run(Expression.Type.Array source) {
        absent();
      }

      @Override
      protected final void run(Expression.Type.Map source) {
        absent();
      }
    }

    /// Класс отвечает за доступ к унарным операторам
    public abstract static class Unary extends Operator<Unary.Proxy> {
      protected abstract void absent2();

      @Override
      protected int modifiers(Proxy value) {
        return value.node().data().modifiers();
      }

      @Override
      protected Extra.DeclarationOperator.Key key(Proxy value) {
        return Extra.DeclarationOperator.Key.of(ast(), value.node()).orElseThrow();
      }

      @Override
      protected final Stream<Unary.Proxy> search(Node<Declaration> node) {
        return node.data().children(ast())
          .gather(Ast.only(Declaration.Operator.class))
          .filter(it -> it.data().operation() == operation() && it.data().parametersId() == 0)
          .filter(it -> Extra.DeclarationOperator.Key.of(ast(), it).isPresent())
          .findFirst()
          .map(Unary.Proxy::new)
          .stream();
      }

      @Override
      protected final void absent() {
        new Accessor.Cast() {
          @Override
          protected Ast ast() {
            return Unary.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return Unary.this.declaration();
          }

          @Override
          protected void absent() {
            Unary.this.absent2();
          }

          @Override
          protected void success2(Stream<? extends Node<Declaration.Cast>> result) {
            var r = result.flatMap(it ->
              runSimple(key(it)).peek(it2 -> it2.cast = it)
            ).toList();
            switch (r.size()) {
              case 0 -> Unary.this.absent2();
              case 1 -> Unary.this.success(r.getFirst());
              default -> Unary.this.ambiguous(r.stream());
            }
          }

          private Stream<? extends Unary.Proxy> runSimple(Expression.Type source) {
            var o = new Operator<Unary.Proxy>() {
              private Stream<? extends Unary.Proxy> result = Stream.of();

              @Override
              protected Ast ast() {
                return Unary.this.ast();
              }

              @Override
              protected Node<Declaration> declaration() {
                return Unary.this.declaration();
              }

              @Override
              protected int modifiers(Proxy value) {
                return value.node().data().modifiers();
              }

              @Override
              protected Extra.DeclarationOperator.Key key(Proxy value) {
                return Extra.DeclarationOperator.Key.of(ast(), value.node()).orElseThrow();
              }

              @Override
              protected Expression.Type target() {
                return Unary.this.target();
              }

              @Override
              protected Operation operation() {
                return Unary.this.operation();
              }

              @Override
              protected Stream<Unary.Proxy> search(Node<Declaration> node) {
                return Unary.this.search(node);
              }

              @Override
              protected void success2(Stream<? extends Unary.Proxy> result) {
                this.result = result;
              }

              @Override
              protected void success(Unary.Proxy result) {
                throw new AssertionError();
              }

              @Override
              protected void ambiguous(Stream<? extends Unary.Proxy> result) {
                throw new AssertionError();
              }

              @Override
              protected void absent() {
              }

              @Override
              protected void run(Expression.Type.Enumeration source) {
                throw new AssertionError();
              }

              @Override
              protected void run(Expression.Type.Array source) {
                throw new AssertionError();
              }

              @Override
              protected void run(Expression.Type.Map source) {
                throw new AssertionError();
              }
            };
            o.run(source);
            return o.result;
          }
        }.run(target());
      }

      @Override
      protected final void success2(Stream<? extends Unary.Proxy> result) {
        result.findFirst().ifPresentOrElse(this::success, this::absent);
      }

      @Override
      protected final void run(Expression.Type.Enumeration source) {
        absent();
      }

      @Override
      protected final void run(Expression.Type.Array source) {
        absent();
      }

      @Override
      protected final void run(Expression.Type.Map source) {
        absent();
      }

      protected static final class Proxy {
        private final Node<Declaration.Operator> node;
        @Nullable Node<Declaration.Cast> cast = null;

        Proxy(Node<Declaration.Operator> node) {
          this.node = node;
        }

        Node<Declaration.Operator> node() {
          return node;
        }

        Optional<Node<Declaration.Cast>> cast() {
          //noinspection NullableProblems
          return Optional.ofNullable(cast);
        }
      }
    }

    /// Класс отвечает за доступ к унарным операторам
    public abstract static class Binary extends Operator<Binary.Proxy> implements CasterMulti.SourceProxy {
      protected abstract Optional<Expression.Type.Declaration> primaryOf(String name);

      protected abstract void absent2();

      @Override
      protected final int modifiers(Proxy value) {
        return switch (value) {
          case ProxyOperator v -> v.node().data().modifiers();
          case ProxySynthetic _ -> Declaration.PUBLIC | Declaration.FINAL;
        };
      }

      @Override
      protected final Extra.DeclarationOperator.Key key(Proxy value) {
        return switch (value) {
          case ProxyOperator v -> Extra.DeclarationOperator.Key.of(ast(), v.node()).orElseThrow();
          case ProxySynthetic v -> new Extra.DeclarationOperator.Key(
            v.synthetic().operation(),
            v.synthetic().parameters()
              .stream()
              .map(Synthetic.Parameter::type)
              .findFirst()
              .orElse(null)
          );
        };
      }

      @Override
      protected final Stream<Binary.ProxyOperator> search(Node<Declaration> node) {
        return node.data().children(ast())
          .gather(Ast.only(Declaration.Operator.class))
          .filter(it -> it.data().operation() == operation() && it.data().parameters(ast()).count() == 1)
          .filter(it -> Extra.DeclarationOperator.Key.of(ast(), it).isPresent())
          .map(Binary.ProxyOperator::new);
      }

      @Override
      protected final void absent() {
        new Accessor.Cast() {
          @Override
          protected Ast ast() {
            return Binary.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return Binary.this.declaration();
          }

          @Override
          protected void absent() {
            Binary.this.absent2();
          }

          @Override
          protected void success2(Stream<? extends Node<Declaration.Cast>> result) {
            new CasterMulti<CasterMulti.SourceProxy, Binary.Proxy>() {
              @Override
              protected Ast ast() {
                return Binary.this.ast();
              }

              @Override
              protected Node<Declaration> declaration() {
                return Binary.this.declaration();
              }

              @Override
              protected Stream<Expression.Type> parameters(Binary.Proxy target) {
                return switch (target) {
                  case ProxyOperator v -> v.node().data()
                    .parameters(ast())
                    .map(it -> ast().typeOf(it.data().definition(ast())).orElseThrow());
                  case ProxySynthetic v -> v.synthetic().parameters()
                    .stream()
                    .map(Synthetic.Parameter::type);
                };
              }

              @Override
              protected void absent() {
                Binary.this.absent2();
              }

              @Override
              protected void ambiguous(Stream<Binary.Proxy> result) {
                Binary.this.ambiguous(result);
              }

              @Override
              protected void success(Binary.Proxy result) {
                Binary.this.success(result);
              }
            }.run(
              List.of(Binary.this),
              result.flatMap(it ->
                runSimple(key(it)).peek(it2 -> it2.cast = it)
              )
            );
          }

          private Stream<? extends Binary.ProxyOperator> runSimple(Expression.Type source) {
            var o = new Operator<Binary.ProxyOperator>() {
              private Stream<? extends Binary.ProxyOperator> result = Stream.of();

              @Override
              protected Ast ast() {
                return Binary.this.ast();
              }

              @Override
              protected Node<Declaration> declaration() {
                return Binary.this.declaration();
              }

              @Override
              protected Expression.Type target() {
                return Binary.this.target();
              }

              @Override
              protected Operation operation() {
                return Binary.this.operation();
              }

              @Override
              protected int modifiers(Binary.ProxyOperator value) {
                return Binary.this.modifiers(value);
              }

              @Override
              protected Object key(Binary.ProxyOperator value) {
                return Binary.this.key(value);
              }

              @Override
              protected Stream<Binary.ProxyOperator> search(Node<Declaration> node) {
                return Binary.this.search(node);
              }

              @Override
              protected void success2(Stream<? extends Binary.ProxyOperator> result) {
                this.result = result;
              }

              @Override
              protected void success(Binary.ProxyOperator result) {
                throw new AssertionError();
              }

              @Override
              protected void ambiguous(Stream<? extends Binary.ProxyOperator> result) {
                throw new AssertionError();
              }

              @Override
              protected void absent() {
              }

              @Override
              protected void run(Expression.Type.Enumeration source) {
                throw new AssertionError();
              }

              @Override
              protected void run(Expression.Type.Array source) {
                throw new AssertionError();
              }

              @Override
              protected void run(Expression.Type.Map source) {
                throw new AssertionError();
              }
            };
            o.run(source);
            return o.result;
          }
        }.run(target());
      }

      @Override
      protected final void run(Expression.Type.Enumeration source) {
        var typeBool = primaryOf("bool").orElse(null);

        if (typeBool == null) absent();
        else switch (operation()) {
          case Operation.EQUAL, Operation.NOT_EQUAL -> success2(Stream.of(
            new ProxySynthetic(new Synthetic.Operator(
              source,
              operation(),
              List.of(new Synthetic.Parameter(source, "value")),
              typeBool
            ))
          ));
          default -> absent();
        }
      }

      @Override
      protected final void run(Expression.Type.Array source) {
        var typeBool = primaryOf("bool").orElse(null);

        if (typeBool == null) absent();
        else switch (operation()) {
          case Operation.ADD -> success2(Stream.of(
            new ProxySynthetic(new Synthetic.Operator(
              source,
              Operation.ADD,
              List.of(new Synthetic.Parameter(source, "elements")),
              source
            ))
          ));
          case Operation.EQUAL, Operation.NOT_EQUAL -> success2(Stream.of(
            new ProxySynthetic(new Synthetic.Operator(
              source,
              operation(),
              List.of(new Synthetic.Parameter(source, "value")),
              typeBool
            ))
          ));
          default -> absent();
        }
      }

      @Override
      protected final void run(Expression.Type.Map source) {
        var typeBool = primaryOf("bool").orElse(null);

        if (typeBool == null) absent();
        else switch (operation()) {
          case Operation.ADD -> success2(Stream.of(
            new ProxySynthetic(new Synthetic.Operator(
              source,
              Operation.ADD,
              List.of(new Synthetic.Parameter(source, "elements")),
              source
            ))
          ));
          case Operation.EQUAL, Operation.NOT_EQUAL -> success2(Stream.of(
            new ProxySynthetic(new Synthetic.Operator(
              source,
              operation(),
              List.of(new Synthetic.Parameter(source, "value")),
              typeBool
            ))
          ));
          default -> absent();
        }
      }

      @Override
      protected final void success2(Stream<? extends Binary.Proxy> result) {
        new CasterMulti<Binary, Binary.Proxy>() {
          @Override
          protected Ast ast() {
            return Binary.this.ast();
          }

          @Override
          protected Node<Declaration> declaration() {
            return Binary.this.declaration();
          }

          @Override
          protected Stream<Expression.Type> parameters(Binary.Proxy target) {
            return switch (target) {
              case ProxyOperator v -> v.node().data()
                .parameters(ast())
                .map(it -> ast().typeOf(it.data().definition(ast())).orElseThrow());
              case ProxySynthetic v -> v.synthetic().parameters()
                .stream()
                .map(Synthetic.Parameter::type);
            };
          }

          @Override
          protected void absent() {
            Binary.this.absent();
          }

          @Override
          protected void ambiguous(Stream<Binary.Proxy> result) {
            Binary.this.ambiguous(result);
          }

          @Override
          protected void success(Binary.Proxy result) {
            Binary.this.success(result);
          }
        }.run(List.of(this), result);
      }

      protected sealed abstract static class Proxy {
      }

      protected static final class ProxyOperator extends Proxy {
        private final Node<Declaration.Operator> node;
        @Nullable Node<Declaration.Cast> cast = null;

        ProxyOperator(Node<Declaration.Operator> node) {
          this.node = node;
        }

        Node<Declaration.Operator> node() {
          return node;
        }

        Optional<Node<Declaration.Cast>> cast() {
          //noinspection NullableProblems
          return Optional.ofNullable(cast);
        }
      }

      protected static final class ProxySynthetic extends Proxy {
        private final Synthetic.Operator synthetic;

        ProxySynthetic(Synthetic.Operator synthetic) {
          this.synthetic = synthetic;
        }

        Synthetic.Operator synthetic() {
          return synthetic;
        }
      }
    }
  }

  /// Класс отвечает за доступ к оператору присвоения
  public abstract static class Priority extends Accessor<Node<Declaration.Priority>> {
    protected abstract void success(Node<Declaration.Priority> result);

    @Override
    protected final int modifiers(Node<Declaration.Priority> value) {
      return value.data().modifiers();
    }

    @Override
    protected final Object key(Node<Declaration.Priority> value) {
      return Runtime.getRuntime();
    }

    @Override
    protected final Stream<Node<Declaration.Priority>> search(Node<Declaration> node) {
      return node.data().children(ast())
        .gather(Ast.only(Declaration.Priority.class))
        .findFirst()
        .stream();
    }

    @Override
    protected final void success2(Stream<? extends Node<Declaration.Priority>> result) {
      result.findFirst().ifPresentOrElse(this::success, this::absent);
    }

    @Override
    protected final void run(Expression.Type.Enumeration source) {
      absent();
    }

    @Override
    protected final void run(Expression.Type.Array source) {
      absent();
    }

    @Override
    protected final void run(Expression.Type.Map source) {
      absent();
    }
  }
}
