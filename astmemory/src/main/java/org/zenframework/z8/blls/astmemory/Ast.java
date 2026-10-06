package org.zenframework.z8.blls.astmemory;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Klass;
import org.zenframework.z8.blls.ast.Node;
import org.zenframework.z8.blls.ast.Token;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.StreamSupport.intStream;
import static java.util.stream.StreamSupport.stream;

public final class Ast implements org.zenframework.z8.blls.ast.Ast {
  // Serialize enums using ordinals for performance
  public static final Set<EnumFeature> JACKSON_FEATURES = unmodifiableSet(EnumSet.of(
    EnumFeature.READ_ENUM_KEYS_USING_INDEX,
    EnumFeature.WRITE_ENUMS_USING_INDEX
  ));
  public static final JacksonModule JACKSON_MODULE = new SimpleModule()
    .addSerializer(Expression.Type.class, new ValueSerializer<>() {
      @Override
      public void serialize(Expression.Type type, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        switch (type) {
          case Expression.Type.Null _ -> gen.writeNull();
          case Expression.Type.Declaration(int id) -> gen.writeNumber(id);
          case Expression.Type.Enumeration(int id) -> gen.writeNumber(-id);
          case Expression.Type.Array(var value) -> {
            gen.writeStartArray();
            serialize(value, gen, ctxt);
            gen.writeEndArray();
          }
          case Expression.Type.Map(var key, var value) -> {
            gen.writeStartArray();
            serialize(key, gen, ctxt);
            serialize(value, gen, ctxt);
            gen.writeEndArray();
          }
        }
      }
    })
    .addDeserializer(Expression.Type.class, new ValueDeserializer<>() {
      @Override
      public Expression.Type getNullValue(DeserializationContext ctxt) {
        return new Expression.Type.Null();
      }

      @Override
      public Expression.Type deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        var result = deserialize(p.readValueAsTree());
        if (result == null) throw DatabindException.from(p, "cannot deserialize Expression.Type");
        else return result;
      }

      private Expression.@Nullable Type deserialize(JsonNode node) {
        return switch (node) {
          case NumericNode v -> v.asInt() > 0
            ? new Expression.Type.Declaration(v.asInt())
            : new Expression.Type.Enumeration(-v.asInt());
          case ArrayNode v -> switch (v.elements().size()) {
            case 1 -> {
              var value = deserialize(v.get(0));
              yield value == null ? null : new Expression.Type.Array(value);
            }
            case 2 -> {
              var key = deserialize(v.get(0));
              var value = deserialize(v.get(1));
              yield key == null || value == null ? null : new Expression.Type.Map(key, value);
            }
            default -> null;
          };
          default -> null;
        };
      }
    });

  private final JsonMapper json;
  private final ArrayList<String> strings = new ArrayList<>(1024);
  private final HashMap<String, Integer> stringsMap = new HashMap<>();
  private final @Nullable Source[] sources = new Source[1024];
  private final ArrayList<Pack_> packs = new ArrayList<>();

  public Ast(JsonMapper json) {
    this.json = json;
    //noinspection DataFlowIssue
    strings.add(null);
    //noinspection DataFlowIssue
    packs.add(null);
    packs.add(new Pack_());
  }

  private static short source(int id) {
    return (short) (id >> 16);
  }

  private static short index(int id) {
    return (short) id;
  }

  private static int id(Source source, short index) {
    return source.id() << 16 | index;
  }

  public JsonMapper json() {
    return json;
  }

  @Override
  public String string(int stringId) {
    assert stringId > 0;
    return strings.get(stringId);
  }

  public int stringSet(String value) {
    return stringsMap.computeIfAbsent(value, _ -> {
      int result = strings.size();
      strings.add(value);
      return result;
    });
  }

  @Override
  public Stream<Token> tokensBack(int tokenId) {
    return tokensBackWhitespace(tokenId).filter(it -> it.kind().ordinal() >= 5);
  }

  @Override
  public Stream<Token> tokensBackWhitespace(int tokenId) {
    assert tokenId > 0;
    return stream(new Spliterator<>() {
      final Source s = sourceOf(source(tokenId));
      short id = index(tokenId + 1);

      @Override
      public boolean tryAdvance(Consumer<? super Token> action) {
        if (id == 0) return false;

        id--;
        action.accept(s.tokens.get(id));

        return true;
      }

      @Override
      public @Nullable Spliterator<Token> trySplit() {
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

  @Override
  public Stream<Token> tokensNextWhitespace(int tokenId) {
    assert tokenId > 0;
    return stream(new Spliterator<>() {
      final Source s = sourceOf(source(tokenId));
      short id = index(tokenId);

      @Override
      public boolean tryAdvance(Consumer<? super Token> action) {
        if (id == s.tokens.size()) return false;

        action.accept(s.tokens.get(id));
        id++;

        return true;
      }

      @Override
      public @Nullable Spliterator<Token> trySplit() {
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

  @Override
  public Stream<Token> tokensNext(int tokenId) {
    return tokensNextWhitespace(tokenId).filter(it -> it.kind().ordinal() >= 5);
  }

  @Override
  public Token token(int tokenId) {
    assert tokenId > 0;

    var s = sourceOf(source(tokenId));
    return s.tokens.get(index(tokenId));
  }

  @Override
  public Node.Data data(int nodeId) {
    assert nodeId > 0;

    var s = sourceOf(source(nodeId));
    return s.entries.get(index(nodeId)).data;
  }

  @Override
  public Token start(Node<?> node) {
    var s = sourceOf(source(node.id()));
    return s.tokens.get(s.entries.get(index(node.id())).startId);
  }

  @Override
  public Token end(Node<?> node) {
    var s = sourceOf(source(node.id()));
    return s.tokens.get(s.entries.get(index(node.id())).endId);
  }

  @Override
  public IntStream parents(int nodeId) {
    assert nodeId > 0;
    return intStream(new Spliterator.OfInt() {
      final Source s = sourceOf(source(nodeId));
      short id = index(nodeId);

      @Override
      public Spliterator.@Nullable OfInt trySplit() {
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

      @Override
      public boolean tryAdvance(IntConsumer action) {
        if (id == 0) return false;

        action.accept(id(s, id));
        id = s.entries.get(id).parentId;

        return true;
      }
    }, false);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <D extends Node.Data> Node<D> parent(int nodeId) {
    assert nodeId > 0;

    var s = sourceOf(source(nodeId));
    short parentId = s.entries.get(index(nodeId)).parentId;
    return new Node<>(id(s, parentId), (D) s.entries.get(parentId).data);
  }

  public void parentSet(int nodeId, int nodeParentId) {
    assert nodeId > 0;
    assert nodeParentId > 0;

    var s = sourceOf(source(nodeId));
    assert s.id() == source(nodeParentId);

    s.entries.get(index(nodeId)).parentId = index(nodeParentId);
  }

  @Override
  public IntStream siblings(int nodeId) {
    if (nodeId == 0) return IntStream.empty();
    else return intStream(new Spliterator.OfInt() {
      final Source s = sourceOf(source(nodeId));
      short id = index(nodeId);

      @Override
      public Spliterator.@Nullable OfInt trySplit() {
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

      @Override
      public boolean tryAdvance(IntConsumer action) {
        if (id == 0) return false;

        action.accept(id(s, id));
        id = s.entries.get(id).siblingId;

        return true;
      }
    }, false);
  }

  public void siblingSet(int nodeId, int nodeSiblingId) {
    assert nodeId > 0;
    assert nodeSiblingId > 0;

    var s = sourceOf(source(nodeId));
    assert s.id() == source(nodeSiblingId);

    s.entries.get(index(nodeId)).siblingId = index(nodeSiblingId);
  }

  @Override
  public Optional<Expression.Type> type(Node<? extends Expression> node) {
    var s = sourceOf(source(node.id()));
    //noinspection NullableProblems
    return Optional.ofNullable(s.entries.get(index(node.id())).type);
  }

  public void typeSet(Node<? extends Expression> node, Expression.Type value) {
    var s = sourceOf(source(node.id()));
    s.entries.get(index(node.id())).type = value;
  }

  @Override
  public <E> E extra(int nodeId, Class<E> klass) {
    assert nodeId > 0;

    var s = sourceOf(source(nodeId));
    return json.treeToValue(s.entries.get(index(nodeId)).extra, klass);
  }

  @Override
  public void extraSet(int nodeId, Object value) {
    assert nodeId > 0;

    var s = sourceOf(source(nodeId));
    var e = s.entries.get(index(nodeId));
    e.extra = json.updateValue(e.extra, value);
  }

  @Override
  public Optional<PackChild> packGet(Ast.Pack pack, String value) {
    return Optional.ofNullable(packs.get(-pack.id()).get(value));
  }

  @Override
  public Stream<Map.Entry<String, PackChild>> packChildren(Ast.Pack pack) {
    return packs.get(-pack.id()).entrySet().stream();
  }

  public Source sourceOf(Path path) {
    int index = IntStream.range(0, sources.length)
      .filter(it -> sources[it] == null)
      .findFirst()
      .orElseThrow();

    var result = new Source((short) (index + 1), path);
    sources[index] = result;
    return result;
  }

  public Source sourceOf(Node<?> node) {
    return sourceOf(source(node.id()));
  }

  public Source sourceOf(short id) {
    //noinspection DataFlowIssue
    return sources[id - 1];
  }

  public Stream<Source> sources() {
    //noinspection NullableProblems
    return Stream.of(sources).filter(Objects::nonNull);
  }

  private static final class Pack_ extends HashMap<String, Ast.PackChild> {
  }

  public final class Source {
    private final short id;
    private final Path path;
    private final ArrayList<Token> tokens = new ArrayList<>(128);
    private final ArrayList<NodeEntry> entries = new ArrayList<>(128);
    private final ArrayList<? super Object> diagnostic = new ArrayList<>();
    private short nodeId;
    private int step;
    private int[] dependencies = new int[0];

    private Source(short id, Path path) {
      this.id = id;
      this.path = path;
      //noinspection DataFlowIssue
      entries.add(null);
    }

    public short id() {
      return id;
    }

    public Path path() {
      return path;
    }

    public Node<org.zenframework.z8.blls.ast.Source> node() {
      return Ast.this.node(Ast.id(this, nodeId));
    }

    public void initialize(Node<org.zenframework.z8.blls.ast.Source> node) {
      var f = new BiFunction<String, PackChild, PackChild>() {
        @Override
        public Ast.PackChild apply(String key, Ast.@Nullable PackChild value) {
          if (value == null) {
            int newPackId = packs.size();
            var p = new Pack_();
            packs.add(p);
            return new Ast.Pack(-newPackId);
          } else {
            return value;
          }
        }
      };
      var c = new Consumer<String>() {
        int packId = 1;

        @Override
        public void accept(String s) {
          if (packs.get(packId).compute(s, f) instanceof Ast.Pack(int id2)) {
            packId = -id2;
          } else {
            new UnsupportedOperationException("TODO: pack/declaration clash diagnostic").printStackTrace();
            System.exit(1);
            throw new AssertionError();
          }
        }
      };
      IntStream.range(1, path.getNameCount() - 1)
        .mapToObj(path()::getName)
        .map(Path::toString)
        .forEach(c);

      node.data().children(Ast.this).forEach(it -> {
        var type = switch (it.data()) {
          case Klass v -> new Ast.Declaration(v.childId());
          case org.zenframework.z8.blls.ast.Enumeration _ -> new Ast.Enumeration(it.id());
          default -> throw new AssertionError();
        };
        String name = switch (it.data()) {
          case Klass v -> v.name(Ast.this);
          case org.zenframework.z8.blls.ast.Enumeration v -> v.name(Ast.this);
          default -> throw new AssertionError();
        };

        packs.get(c.packId).merge(name, type, (_, _) -> {
          new UnsupportedOperationException("TODO: declaration/declaration clash diagnostic").printStackTrace();
          System.exit(1);
          throw new AssertionError();
        });

        switch (it.data()) {
          case Klass _ -> {
            var e = new Extra.Declaration();
            e.packId = -c.packId;
            extraSet(type.id(), e);
          }
          case org.zenframework.z8.blls.ast.Enumeration _ -> {
            var e = new Extra.Enumeration();
            e.packId = -c.packId;
            extraSet(type.id(), e);
          }
          default -> throw new AssertionError();
        }
      });

      {
        var e = new Extra.Source();
        e.packId = -c.packId;
        extraSet(node.id(), e);
      }

      nodeId = index(node.id());
    }

    public int getStep() {
      return step;
    }

    public void setStep(int value) {
      step = value;
    }

    public Stream<Source> dependencies() {
      return Stream.concat(Stream.of(this), Arrays.stream(dependencies).mapToObj(it -> sourceOf((short) it)));
    }

    public int[] getDependencies() {
      return dependencies;
    }

    public void setDependencies(int[] value) {
      dependencies = value;
    }

    public List<?> getDiagnostic() {
      return diagnostic;
    }

    public void setDiagnostic(Object value) {
      diagnostic.add(value);
    }

    public void clearDiagnostic() {
      diagnostic.clear();
    }

    public void clearNodes() {
      if (getStep() == 0) return;

      int packId = extra(node().id(), Extra.Source.class).packId;

      entries.stream()
        .skip(1)
        .forEach(it -> {
          it.type = null;
          it.extra = json.createObjectNode();
        });

      {
        var e = new Extra.Source();
        e.packId = packId;
        extraSet(node().id(), e);
      }

      node().data()
        .children(Ast.this)
        .forEach(it -> {
          switch (it.data()) {
            case Klass v -> {
              var e = new Extra.Declaration();
              e.packId = packId;
              extraSet(v.childId(), e);
            }
            case org.zenframework.z8.blls.ast.Enumeration _ -> {
              var e = new Extra.Enumeration();
              e.packId = packId;
              extraSet(it.id(), e);
            }
            default -> throw new AssertionError();
          }
        });
    }

    public void delete() {
      if (getStep() > 0) {
        int packId = extra(node().id(), Extra.Source.class).packId;
        node().data()
          .children(Ast.this)
          .map(it -> switch (it.data()) {
            case Klass v -> v.name(Ast.this);
            case org.zenframework.z8.blls.ast.Enumeration v -> v.name(Ast.this);
            default -> throw new AssertionError();
          })
          .forEach(packs.get(-packId)::remove);
      }
      sources[id() - 1] = null;
    }

    public int tokenSet(Token value) {
      int result = Ast.id(this, (short) tokens.size());
      tokens.add(new Token(result, value.kind(), value.line(), value.character(), value.index()));
      return result;
    }

    public int nodeSet(int nodeStartId, int nodeEndId, Node.Data data) {
      assert nodeStartId > 0;
      assert nodeEndId > 0;

      assert id() == source(nodeStartId);
      assert id() == source(nodeEndId);

      short result = (short) entries.size();
      entries.add(new NodeEntry(data, index(nodeStartId), index(nodeEndId), json.createObjectNode()));
      return Ast.id(this, result);
    }

    private static final class NodeEntry {
      private final Node.Data data;
      private final short startId;
      private final short endId;
      private ObjectNode extra;
      private short parentId;
      private short siblingId;
      private Expression.@Nullable Type type;

      private NodeEntry(Node.Data data, short startId, short endId, ObjectNode extra) {
        this.data = data;
        this.startId = startId;
        this.endId = endId;
        this.extra = extra;
      }
    }
  }
}
