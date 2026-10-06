package org.zenframework.z8.blls.astsqlite;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.StreamSupport.intStream;
import static java.util.stream.StreamSupport.stream;

public abstract class Ast<S extends Ast<?>.Source> implements org.zenframework.z8.blls.ast.Ast, AutoCloseable {
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
  private static final JsonPointer JSON_POINTER_TYPE = JsonPointer.compile("/type");
  private final JsonMapper json;
  private final Sqlite3.Stmt setQueryOnlyTrue;
  private final Sqlite3.Stmt setQueryOnlyFalse;
  private final Sqlite3.Stmt string;
  private final Sqlite3.Stmt stringSet;
  private final Sqlite3.Stmt tokenBack;
  private final Sqlite3.Stmt tokenBackWhitespace;
  private final Sqlite3.Stmt tokenNextWhitespace;
  private final Sqlite3.Stmt tokenNext;
  private final Sqlite3.Stmt tokenSet;
  private final Sqlite3.Stmt node;
  private final Sqlite3.Stmt nodeSet;
  private final Sqlite3.Stmt start;
  private final Sqlite3.Stmt end;
  private final Sqlite3.Stmt parent;
  private final Sqlite3.Stmt parentSet;
  private final Sqlite3.Stmt dataSet;
  private final Sqlite3.Stmt sourcesDirty;
  private final Sqlite3.Stmt sourcesClean;
  private final Sqlite3.Stmt sources;
  private final Sqlite3.Stmt sourceOfPath;
  private final Sqlite3.Stmt sourceOfNode;
  private final Sqlite3.Stmt sourceSet;
  private final Sqlite3.Stmt sourcePath;
  private final Sqlite3.Stmt sourcePack;
  private final Sqlite3.Stmt sourceMd5;
  private final Sqlite3.Stmt sourceNodeId;
  private final Sqlite3.Stmt sourceNodeIdSet;
  private final Sqlite3.Stmt sourceStep;
  private final Sqlite3.Stmt sourceStepSet;
  private final Sqlite3.Stmt sourceDiagnostic;
  private final Sqlite3.Stmt sourceDiagnosticSet;
  private final Sqlite3.Stmt sourceDiagnosticId;
  private final Sqlite3.Stmt sourceDiagnosticIdSet;
  private final Sqlite3.Stmt sourceDiagnosticClear;
  private final Sqlite3.Stmt sourceDependencySet;
  private final Sqlite3.Stmt sourceDependencies;
  private final Sqlite3.Stmt sourceDelete;
  private final Sqlite3.Stmt pack;
  private final Sqlite3.Stmt packSet;
  private final Sqlite3.Stmt packSetChild;
  private final Sqlite3.Stmt packChildren;

  public Ast(JsonMapper json, Sqlite3.Db db) {
    this.json = json;
    db.exec("""
      PRAGMA foreign_keys = ON;
      PRAGMA temp_store = MEMORY;
      PRAGMA synchronous = OFF;
      PRAGMA journal_mode = OFF;
      CREATE TABLE string (
        rowid INTEGER PRIMARY KEY,
        value TEXT NOT NULL UNIQUE
      ) STRICT;
      CREATE TABLE source_to_string (
        source_id INTEGER NOT NULL REFERENCES source (rowid) ON DELETE CASCADE,
        string_id INTEGER NOT NULL REFERENCES string (rowid) ON DELETE RESTRICT,
        PRIMARY KEY (source_id, string_id)
      ) STRICT, WITHOUT ROWID;
      CREATE TRIGGER string_cleanup AFTER DELETE ON source_to_string
      BEGIN
        DELETE FROM string WHERE NOT EXISTS (
          SELECT 1 FROM source_to_string WHERE string_id = rowid
        );
      END;
      CREATE TABLE token (
        rowid INTEGER PRIMARY KEY,
        source_id INTEGER NOT NULL REFERENCES source (rowid) ON DELETE CASCADE,
        kind INTEGER NOT NULL,
        line INTEGER NOT NULL,
        character INTEGER NOT NULL,
        index_ INTEGER NOT NULL,
        UNIQUE (source_id, index_)
      ) STRICT;
      CREATE INDEX token_source_id ON token (source_id);
      CREATE TABLE node (
        rowid INTEGER PRIMARY KEY,
        source_id INTEGER NOT NULL REFERENCES source (rowid) ON DELETE CASCADE,
        start_id INTEGER NOT NULL REFERENCES token (rowid) ON DELETE RESTRICT,
        end_id INTEGER NOT NULL REFERENCES token (rowid) ON DELETE RESTRICT,
        parent_id INTEGER REFERENCES node (rowid) ON DELETE SET NULL,
        kind INTEGER NOT NULL,
        data TEXT NOT NULL
      ) STRICT;
      CREATE INDEX node_source_id ON node (source_id);
      CREATE INDEX node_start_id ON node (start_id);
      CREATE INDEX node_end_id ON node (end_id);
      CREATE TABLE source (
        rowid INTEGER PRIMARY KEY,
        path TEXT NOT NULL UNIQUE,
        md5 BLOB NOT NULL,
        pack_id INTEGER DEFAULT NULL REFERENCES pack (rowid) ON DELETE RESTRICT,
        node_id INTEGER DEFAULT NULL REFERENCES node (rowid) ON DELETE RESTRICT,
        step INTEGER NOT NULL DEFAULT 0,
        diagnostic TEXT NOT NULL DEFAULT '[]',
        diagnostic_least INTEGER NOT NULL DEFAULT 0,
        diagnostic_most INTEGER NOT NULL DEFAULT 0
      ) STRICT;
      CREATE TABLE source_dependency (
        rowid INTEGER PRIMARY KEY REFERENCES source (rowid) ON DELETE CASCADE
      ) STRICT;
      CREATE TABLE source_to_source (
        a_id INTEGER NOT NULL REFERENCES source_dependency (rowid) ON DELETE CASCADE,
        b_id INTEGER NOT NULL REFERENCES source_dependency (rowid) ON DELETE CASCADE,
        PRIMARY KEY (a_id, b_id)
      ) STRICT, WITHOUT ROWID;
      CREATE TRIGGER source_mark_dirty AFTER DELETE ON source_to_source
      BEGIN
        DELETE FROM source_dependency WHERE rowid = OLD.a_id;
        UPDATE source SET step = 1, diagnostic = '[]', diagnostic_least = 0, diagnostic_most = 0 WHERE rowid = OLD.a_id;
      END;
      CREATE TABLE pack (
        rowid INTEGER PRIMARY KEY
      ) STRICT;
      CREATE TABLE pack_to_children (
        pack_id INTEGER NOT NULL REFERENCES pack (rowid) ON DELETE CASCADE,
        string TEXT NOT NULL,
        child_pack_id INTEGER REFERENCES pack (rowid) ON DELETE CASCADE,
        child_node_id INTEGER REFERENCES node (rowid) ON DELETE CASCADE,
        PRIMARY KEY (pack_id, string)
      ) STRICT, WITHOUT ROWID;
      CREATE INDEX pack_to_children_pack_id ON pack_to_children (pack_id);
      CREATE INDEX pack_to_children_child_pack_id ON pack_to_children (child_pack_id);
      CREATE INDEX pack_to_children_child_node_id ON pack_to_children (child_node_id);
      INSERT INTO pack VALUES (1)
      """);
    setQueryOnlyTrue = db.prepare("PRAGMA query_only = 1", Sqlite3.Prepare.PERSISTENT);
    setQueryOnlyFalse = db.prepare("PRAGMA query_only = 0", Sqlite3.Prepare.PERSISTENT);
    string = db.prepare("SELECT value FROM string WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    stringSet = db.prepare("""
      INSERT INTO string VALUES (NULL, ?1)
      ON CONFLICT DO UPDATE SET rowid = rowid
      RETURNING rowid
      """, Sqlite3.Prepare.PERSISTENT);
    tokenBack = db.prepare("""
      SELECT t1.rowid, t1.kind, t1.line, t1.character, t1.index_
      FROM token t1
      JOIN token t2 ON t1.source_id = t2.source_id AND t1.index_ <= t2.index_
      WHERE t2.rowid = ? AND t1.kind >= 5
      ORDER BY t1.index_ DESC
      """, Sqlite3.Prepare.PERSISTENT);
    tokenBackWhitespace = db.prepare("""
      SELECT t1.rowid, t1.kind, t1.line, t1.character, t1.index_
      FROM token t1
      JOIN token t2 ON t1.source_id = t2.source_id AND t1.index_ <= t2.index_
      WHERE t2.rowid = ?
      ORDER BY t1.index_ DESC
      """, Sqlite3.Prepare.PERSISTENT);
    tokenNextWhitespace = db.prepare("""
      SELECT t1.rowid, t1.kind, t1.line, t1.character, t1.index_
      FROM token t1
      JOIN token t2 ON t1.source_id = t2.source_id AND t1.index_ >= t2.index_
      WHERE t2.rowid = ?
      ORDER BY t1.index_ ASC
      """, Sqlite3.Prepare.PERSISTENT);
    tokenNext = db.prepare("""
      SELECT t1.rowid, t1.kind, t1.line, t1.character, t1.index_
      FROM token t1
      JOIN token t2 ON t1.source_id = t2.source_id AND t1.index_ >= t2.index_
      WHERE t2.rowid = ? AND t1.kind >= 5
      ORDER BY t1.index_ ASC
      """, Sqlite3.Prepare.PERSISTENT);
    tokenSet = db.prepare("INSERT INTO token VALUES (NULL, ?, ?, ?, ?, ?) RETURNING rowid", Sqlite3.Prepare.PERSISTENT);
    node = db.prepare("SELECT kind, data FROM node WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    nodeSet = db.prepare("""
      INSERT INTO node (rowid, source_id, start_id, end_id, kind, data)
      VALUES (NULL, ?, ?, ?, ?, ?)
      RETURNING rowid
      """, Sqlite3.Prepare.PERSISTENT);
    start = db.prepare("""
      SELECT t1.rowid, t1.kind, t1.line, t1.character, t1.index_
      FROM node n1
      JOIN token t1 ON t1.rowid = n1.start_id
      WHERE n1.rowid = ?
      """, Sqlite3.Prepare.PERSISTENT);
    end = db.prepare("""
      SELECT t1.rowid, t1.kind, t1.line, t1.character, t1.index_
      FROM node n1
      JOIN token t1 ON t1.rowid = n1.end_id
      WHERE n1.rowid = ?
      """, Sqlite3.Prepare.PERSISTENT);
    parent = db.prepare("SELECT parent_id FROM node WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    parentSet = db.prepare("UPDATE node SET parent_id = ? WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    dataSet = db.prepare("UPDATE node SET data = ? WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourcesDirty = db.prepare("""
      WITH sources_dirty(rowid) AS (
        SELECT source.rowid FROM source
        WHERE NOT EXISTS (
          SELECT *
          FROM source_dependency
          WHERE source_dependency.rowid = source.rowid
        )
      )
      SELECT COUNT(*) FROM sources_dirty
      UNION ALL
      SELECT rowid FROM sources_dirty
      """, Sqlite3.Prepare.PERSISTENT);
    sourcesClean = db.prepare("""
      INSERT OR IGNORE INTO source_dependency
      SELECT rowid FROM source
      """, Sqlite3.Prepare.PERSISTENT);
    sources = db.prepare("""
      SELECT COUNT(*) FROM source
      UNION ALL
      SELECT rowid FROM source
      """, Sqlite3.Prepare.PERSISTENT);
    sourceOfPath = db.prepare("SELECT rowid FROM source WHERE path = ?", Sqlite3.Prepare.PERSISTENT);
    sourceOfNode = db.prepare("SELECT source_id FROM node WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceSet = db.prepare("""
      INSERT INTO source (rowid, path, md5, pack_id)
      VALUES (NULL, ?, ?, ?)
      RETURNING rowid
      """, Sqlite3.Prepare.PERSISTENT);
    sourcePath = db.prepare("SELECT path FROM source WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourcePack = db.prepare("SELECT pack_id FROM source WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceMd5 = db.prepare("SELECT md5 FROM source WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceNodeId = db.prepare("SELECT node_id FROM source WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceNodeIdSet = db.prepare("UPDATE source SET node_id = ? WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceStep = db.prepare("SELECT step FROM source WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceStepSet = db.prepare("UPDATE source SET step = ? WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceDiagnostic = db.prepare("SELECT diagnostic FROM source WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceDiagnosticSet = db.prepare("""
      UPDATE source
      SET diagnostic = json_insert(diagnostic, '$[#]', jsonb(?)), diagnostic_least = 0, diagnostic_most = 0
      WHERE rowid = ?
      """, Sqlite3.Prepare.PERSISTENT);
    sourceDiagnosticId = db.prepare("SELECT diagnostic_least, diagnostic_most FROM source WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceDiagnosticIdSet = db.prepare("UPDATE source SET diagnostic_least = ?, diagnostic_most = ? WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    sourceDiagnosticClear = db.prepare("""
      UPDATE source
      SET diagnostic = '[]', diagnostic_least = 0, diagnostic_most = 0
      WHERE rowid = ?
      """, Sqlite3.Prepare.PERSISTENT);
    sourceDependencySet = db.prepare("INSERT OR IGNORE INTO source_to_source VALUES (?, ?)", Sqlite3.Prepare.PERSISTENT);
    sourceDependencies = db.prepare("""
      WITH RECURSIVE source_dependencies(rowid) AS (
        SELECT rowid
        FROM source_dependency
        WHERE source_dependency.rowid = ?
        UNION
        SELECT b_id
        FROM source_to_source
        JOIN source_dependencies
        WHERE source_to_source.a_id = source_dependencies.rowid
      )
      SELECT COUNT(*) FROM source_dependencies
      UNION ALL
      SELECT rowid FROM source_dependencies
      """, Sqlite3.Prepare.PERSISTENT);
    sourceDelete = db.prepare("DELETE FROM source WHERE rowid = ?", Sqlite3.Prepare.PERSISTENT);
    pack = db.prepare("""
      SELECT child_pack_id, child_node_id, kind
      FROM pack_to_children
      LEFT JOIN node ON node.rowid = child_node_id
      WHERE pack_id = ? AND string = ?
      """, Sqlite3.Prepare.PERSISTENT);
    packSet = db.prepare("INSERT INTO pack VALUES (NULL) RETURNING rowid", Sqlite3.Prepare.PERSISTENT);
    packSetChild = db.prepare("""
      INSERT INTO pack_to_children
      VALUES (?1, ?2, ?3, ?4)
      ON CONFLICT DO UPDATE SET child_pack_id = ?3, child_node_id = ?4
      """, Sqlite3.Prepare.PERSISTENT);
    packChildren = db.prepare("""
      SELECT COUNT(*), NULL, NULL, NULL
      FROM pack_to_children
      WHERE pack_id = ?1
      UNION ALL
      SELECT string, child_pack_id, child_node_id, kind
      FROM pack_to_children
      LEFT JOIN node ON node.rowid = child_node_id
      WHERE pack_id = ?1
      """, Sqlite3.Prepare.PERSISTENT);
  }

  protected abstract S sourceOf(int sourceId);

  @Override
  public void close() {
    setQueryOnlyTrue.close();
    setQueryOnlyFalse.close();
    string.close();
    stringSet.close();
    tokenBack.close();
    tokenBackWhitespace.close();
    tokenNextWhitespace.close();
    tokenNext.close();
    tokenSet.close();
    node.close();
    nodeSet.close();
    start.close();
    end.close();
    parent.close();
    parentSet.close();
    dataSet.close();
    sourcesDirty.close();
    sourcesClean.close();
    sources.close();
    sourceOfPath.close();
    sourceOfNode.close();
    sourceSet.close();
    sourcePath.close();
    sourcePack.close();
    sourceMd5.close();
    sourceNodeId.close();
    sourceNodeIdSet.close();
    sourceStep.close();
    sourceStepSet.close();
    sourceDiagnostic.close();
    sourceDiagnosticSet.close();
    sourceDiagnosticId.close();
    sourceDiagnosticIdSet.close();
    sourceDiagnosticClear.close();
    sourceDependencySet.close();
    sourceDependencies.close();
    sourceDelete.close();
    pack.close();
    packSet.close();
    packSetChild.close();
    packChildren.close();
  }

  public final Optional<S> sourceOf(Path path) {
    var q = new Query(sourceOfPath);
    q.set(path.toString());

    if (q.step()) {
      int result = q.get();
      q.stepEnd();
      return Optional.of(sourceOf(result));
    } else return Optional.empty();
  }

  public final Stream<S> sourcesDirty() {
    var q = new Query(sourcesDirty);

    q.stepNext();
    int size = q.get();

    return stream(new Spliterator<>() {
      private boolean hasNext = q.step();

      @Override
      public boolean tryAdvance(Consumer<? super S> action) {
        if (!hasNext) return false;

        action.accept(sourceOf(q.get()));
        hasNext = q.step();
        return true;
      }

      @Override
      public @Nullable Spliterator<S> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return size;
      }

      @Override
      public int characteristics() {
        return DISTINCT | SIZED | NONNULL | IMMUTABLE;
      }
    }, false);
  }

  public final void sourcesClean() {
    var q = new Query(sourcesClean);

    q.stepEnd();
  }

  public final Stream<S> sources() {
    var q = new Query(sources);

    q.stepNext();
    int size = q.get();

    return stream(new Spliterator<>() {
      private boolean hasNext = q.step();

      @Override
      public boolean tryAdvance(Consumer<? super S> action) {
        if (!hasNext) return false;

        action.accept(sourceOf(q.get()));
        hasNext = q.step();
        return true;
      }

      @Override
      public @Nullable Spliterator<S> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return size;
      }

      @Override
      public int characteristics() {
        return DISTINCT | SIZED | NONNULL | IMMUTABLE;
      }
    }, false);
  }

  public final S sourceOf(Node<?> node) {
    var q = new Query(sourceOfNode);
    q.set(node.id());

    q.stepNext();
    int result = q.get();
    q.stepEnd();
    return sourceOf(result);
  }

  public final S sourceOf(Path path, byte[] digest) {
    assert digest.length == 16;

    int packId = -Ast.Pack.ROOT.id();
    var pack = IntStream.range(1, path.getNameCount() - 1)
      .mapToObj(path::getName)
      .map(Path::toString)
      .iterator();
    while (pack.hasNext()) {
      var p = pack.next();

      var q = new Query(this.pack);
      q.set(packId);
      q.set(p);

      if (q.step()) {
        int childPackId = q.get();
        int childNodeId = q.get();
        q.stepEnd();

        if (childPackId == 0) {
          // TODO: pack/declaration clash diagnostic
          assert childNodeId != 0;

          q = new Query(packSet);

          q.stepNext();
          int newPackId = q.get();
          q.stepEnd();

          q = new Query(packSetChild);
          q.set(packId);
          q.set(p);
          q.set(newPackId);
          q.set(childNodeId);

          q.stepEnd();

          packId = newPackId;
        } else {
          packId = childPackId;
        }
      } else {
        q = new Query(packSet);

        q.stepNext();
        int newPackId = q.get();
        q.stepEnd();

        q = new Query(packSetChild);
        q.set(packId);
        q.set(p);
        q.set(newPackId);
        q.setNull();

        q.stepEnd();

        packId = newPackId;
      }
    }

    var q = new Query(sourceSet);
    q.set(path.toString());
    q.set(digest);
    q.set(packId);

    q.stepNext();
    int result = q.get();
    q.stepEnd();

    return sourceOf(result);
  }

  public final void setQueryOnly(boolean value) {
    var q = new Query(value ? setQueryOnlyTrue : setQueryOnlyFalse);

    q.stepEnd();
  }

  @Override
  public final String string(int stringId) {
    assert stringId > 0;

    var q = new Query(string);
    q.set(stringId);

    q.stepNext();
    String result = q.getString().orElseThrow();
    q.stepEnd();
    return result;
  }

  @Override
  public final Stream<Token> tokensBack(int tokenId) {
    assert tokenId > 0;

    return stream(new SpliteratorTokens(tokenBack, tokenId), false);
  }

  @Override
  public final Stream<Token> tokensBackWhitespace(int tokenId) {
    assert tokenId > 0;

    return stream(new SpliteratorTokens(tokenBackWhitespace, tokenId), false);
  }

  @Override
  public final Stream<Token> tokensNextWhitespace(int tokenId) {
    assert tokenId > 0;

    return stream(new SpliteratorTokens(tokenNextWhitespace, tokenId), false);
  }

  @Override
  public final Stream<Token> tokensNext(int tokenId) {
    assert tokenId > 0;

    return stream(new SpliteratorTokens(tokenNext, tokenId), false);
  }

  @Override
  public final Node.Data data(int nodeId) {
    assert nodeId > 0;

    var q = new Query(node);
    q.set(nodeId);

    q.stepNext();
    var nodeKind = Kind.values()[q.get()];
    String data = q.getString().orElseThrow();
    q.stepEnd();
    try (var parser = json.createParser(data)) {
      return parser.readValueAs(nodeKind.klass);
    }
  }

  @Override
  public final Token start(Node<?> node) {
    var q = new Query(start);
    q.set(node.id());

    q.stepNext();
    var result = q.getToken();
    q.stepEnd();
    return result;
  }

  @Override
  public final Token end(Node<?> node) {
    var q = new Query(end);
    q.set(node.id());

    q.stepNext();
    var result = q.getToken();
    q.stepEnd();
    return result;
  }

  @Override
  public final IntStream parents(int nodeId) {
    assert nodeId > 0;

    return intStream(new Spliterator.OfInt() {
      private int id = nodeId;

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

        var q = new Query(parent);
        q.set(id);

        q.stepNext();
        int newId = q.get();
        q.stepEnd();

        action.accept(id);
        id = newId;
        return true;
      }
    }, false);
  }

  @Override
  public final IntStream siblings(int nodeId) {
    return intStream(new Spliterator.OfInt() {
      private int id = nodeId;

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

        var q = new Query(node);
        q.set(id);

        q.stepNext();
        var _ = q.get();
        String data = q.getString().orElseThrow();
        q.stepEnd();

        action.accept(id);
        try (var parser = json.createParser(data)) {
          var siblingId = parser.<JsonNode>readValueAsTree().get("siblingId");
          id = siblingId == null ? 0 : siblingId.asInt(0);
        }
        return true;
      }
    }, false);
  }

  @Override
  public final Optional<Expression.Type> type(Node<? extends Expression> node) {
    var q = new Query(this.node);
    q.set(node.id());

    q.stepNext();
    var _ = q.get();
    String data = q.getString().orElse(null);
    q.stepEnd();

    if (data == null) return Optional.empty();
    else try (var parser = json.createParser(data)) {
      return Optional.ofNullable(parser.<ObjectNode>readValueAsTree().get("type"))
        .map(it -> json.treeToValue(it, Expression.Type.class));
    }
  }

  public final void typeSet(Node<? extends Expression> node, Expression.Type value) {
    var q = new Query(this.node);
    q.set(node.id());

    q.stepNext();
    var _ = q.get();
    String data = q.getString().orElseThrow();
    q.stepEnd();

    try (var parser = json.createParser(data)) {
      q = new Query(dataSet);
      q.set(
        json.writer().writeValueAsString(
          parser.<ObjectNode>readValueAsTree().put(
            JSON_POINTER_TYPE, json.valueToTree(value)
          )
        )
      );
      q.set(node.id());

      q.stepEnd();
    }
  }

  @Override
  public final <E> E extra(int nodeId, Class<E> klass) {
    assert nodeId > 0;

    var q = new Query(node);
    q.set(nodeId);

    q.stepNext();
    var _ = q.get();
    String data = q.getString().orElseThrow();
    q.stepEnd();

    return json.readValue(data, klass);
  }

  @Override
  public final void extraSet(int nodeId, Object value) {
    assert nodeId > 0;

    var q = new Query(node);
    q.set(nodeId);

    q.stepNext();
    var _ = q.get();
    String data = q.getString().orElseThrow();
    q.stepEnd();

    try (var parser = json.createParser(data)) {
      ObjectNode dataNode = parser.readValueAsTree();

      q = new Query(dataSet);
      q.set(
        json.writer().writeValueAsString(
          json.updateValue(dataNode, value)
        )
      );
      q.set(nodeId);

      q.stepEnd();
    }
  }

  @Override
  public final Optional<Ast.PackChild> packGet(Ast.Pack pack, String value) {
    var q = new Query(Ast.this.pack);
    q.set(-pack.id());
    q.set(value);

    if (q.step()) {
      int childPackId = q.get();
      int childNodeId = q.get();
      var childNodeKind = Kind.values()[q.get()];
      q.stepEnd();

      return Optional.of(
        childNodeId > 0
          ? switch (childNodeKind) {
          case Kind.DECLARATION -> new Ast.Declaration(childNodeId);
          case Kind.ENUMERATION -> new Ast.Enumeration(childNodeId);
          default -> throw new AssertionError();
        }
          : new Ast.Pack(-childPackId)
      );
    } else return Optional.empty();
  }

  @Override
  public final Stream<Map.Entry<String, Ast.PackChild>> packChildren(Ast.Pack pack) {
    var q = new Query(packChildren);
    q.set(-pack.id());

    q.stepNext();
    int size = q.get();

    return stream(new Spliterator<>() {
      private boolean hasNext = q.step();

      @Override
      public boolean tryAdvance(Consumer<? super Map.Entry<String, Ast.PackChild>> action) {
        if (!hasNext) return false;

        String string = q.getString().orElseThrow();
        int childPackId = q.get();
        int childNodeId = q.get();
        var childNodeKind = Kind.values()[q.get()];
        action.accept(Map.entry(
          string,
          childNodeId > 0
            ? switch (childNodeKind) {
            case Kind.DECLARATION -> new Ast.Declaration(childNodeId);
            case Kind.ENUMERATION -> new Ast.Enumeration(childNodeId);
            default -> throw new AssertionError();
          }
            : new Ast.Pack(-childPackId)
        ));
        hasNext = q.step();
        return true;
      }

      @Override
      public @Nullable Spliterator<Map.Entry<String, Ast.PackChild>> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return size;
      }

      @Override
      public int characteristics() {
        return ORDERED | DISTINCT | SIZED | NONNULL | IMMUTABLE;
      }
    }, false);
  }

  private enum Kind {
    SOURCE(org.zenframework.z8.blls.ast.Source.class),
    IMPORT(Import.class),
    KLASS(Klass.class),
    ENUMERATION(org.zenframework.z8.blls.ast.Enumeration.class),
    ANNOTATION(Annotation.class),
    PARAMETER(Parameter.class),
    DECLARATION(org.zenframework.z8.blls.ast.Declaration.class),
    DECLARATION_FIELD(org.zenframework.z8.blls.ast.Declaration.Field.class),
    DECLARATION_INITIALIZE(org.zenframework.z8.blls.ast.Declaration.Initialize.class),
    DECLARATION_METHOD(org.zenframework.z8.blls.ast.Declaration.Method.class),
    DECLARATION_PRIORITY(org.zenframework.z8.blls.ast.Declaration.Priority.class),
    DECLARATION_CAST(org.zenframework.z8.blls.ast.Declaration.Cast.class),
    DECLARATION_OPERATOR(org.zenframework.z8.blls.ast.Declaration.Operator.class),
    DECLARATION_RECORDS(org.zenframework.z8.blls.ast.Declaration.Records.class),
    DECLARATION_RECORDS_CHILD(org.zenframework.z8.blls.ast.Declaration.Records.Child.class),
    ENUMERATION_CHILD(org.zenframework.z8.blls.ast.Enumeration.Child.class),
    STATEMENT_BLOCK(Statement.Block.class),
    STATEMENT_EXPRESSION(Statement.Expression.class),
    STATEMENT_VARIABLE(Statement.Variable.class),
    STATEMENT_RETURN(Statement.Return.class),
    STATEMENT_BREAK(Statement.Break.class),
    STATEMENT_CONTINUE(Statement.Continue.class),
    STATEMENT_THROW(Statement.Throw.class),
    STATEMENT_TRY(Statement.Try.class),
    STATEMENT_CONDITION(Statement.Condition.class),
    STATEMENT_LOOPDO(Statement.LoopDo.class),
    STATEMENT_LOOPWHILE(Statement.LoopWhile.class),
    STATEMENT_LOOPFOR(Statement.LoopFor.class),
    STATEMENT_LOOPFOREACH(Statement.LoopForEach.class),
    STATEMENT_TRY_CATCH(Statement.Try.Catch.class),
    EXPRESSION_PAREN(Expression.Paren.class),
    EXPRESSION_NULL(Expression.Null.class),
    EXPRESSION_BOOLEAN(Expression.Boolean.class),
    EXPRESSION_INTEGER(Expression.Integer.class),
    EXPRESSION_DECIMAL(Expression.Decimal.class),
    EXPRESSION_STRING(Expression.String.class),
    EXPRESSION_SPECIAL(Expression.Special.class),
    EXPRESSION_THIS(Expression.This.class),
    EXPRESSION_SUPER(Expression.Super.class),
    EXPRESSION_CONTAINER(Expression.Container.class),
    EXPRESSION_NEW(Expression.New.class),
    EXPRESSION_ARRAY(Expression.Array.class),
    EXPRESSION_ACCESS(Expression.Access.class),
    EXPRESSION_INDEX(Expression.Index.class),
    EXPRESSION_INVOKE(Expression.Invoke.class),
    EXPRESSION_CAST(Expression.Cast.class),
    EXPRESSION_OBJECT(Expression.Object.class),
    EXPRESSION_UNARY(Expression.Unary.class),
    EXPRESSION_BINARY(Expression.Binary.class),
    EXPRESSION_INSTANCEOF(Expression.Instanceof.class),
    EXPRESSION_TERNARY(Expression.Ternary.class),
    EXPRESSION_ARRAY_CHILD_VALUE(Expression.Array.Child.Value.class),
    EXPRESSION_ARRAY_CHILD_ENTRY(Expression.Array.Child.Entry.class),
    EXPRESSION_INVOKE_ARGUMENT(Expression.Invoke.Argument.class),
    DEFINITION_REFERENCE(Definition.Reference.class),
    DEFINITION_ARRAY(Definition.Array.class),
    DEFINITION_MAP(Definition.Map.class);

    final Class<? extends Node.Data> klass;

    Kind(Class<? extends Node.Data> klass) {
      this.klass = klass;
    }
  }

  private static final class Query {
    private final Sqlite3.Stmt stmt;
    private int bindIndex = 1;
    private int columnIndex = 0;

    private Query(Sqlite3.Stmt stmt) {
      stmt.clearBindings();
      stmt.reset();
      this.stmt = stmt;
    }

    private void setNull() {
      stmt.bindNull(bindIndex);
      bindIndex++;
    }

    private void set(long value) {
      stmt.bindInt64(bindIndex, value);
      bindIndex++;
    }

    private void set(byte[] value) {
      stmt.bindBlob(bindIndex, value);
      bindIndex++;
    }

    private void set(String value) {
      stmt.bindText(bindIndex, value);
      bindIndex++;
    }

    private boolean step() {
      columnIndex = 0;
      return stmt.step();
    }

    private void stepNext() {
      if (!step()) throw new AssertionError();
    }

    private void stepEnd() {
      if (step()) throw new AssertionError();
    }

    private int get() {
      int result = (int) stmt.columnInt64(columnIndex);
      columnIndex++;
      return result;
    }

    private long getLong() {
      long result = stmt.columnInt64(columnIndex);
      columnIndex++;
      return result;
    }

    private Optional<byte[]> getBlob() {
      var result = stmt.columnBlob(columnIndex);
      columnIndex++;
      return result;
    }

    private Optional<String> getString() {
      var result = stmt.columnText(columnIndex);
      columnIndex++;
      return result;
    }

    private Token getToken() {
      return new Token(get(), Token.Kind.values()[get()], get(), get(), get());
    }
  }

  private static final class SpliteratorTokens implements Spliterator<Token> {
    private final Query q;
    private boolean hasNext;

    private SpliteratorTokens(Sqlite3.Stmt stmt, int tokenId) {
      q = new Query(stmt);
      q.set(tokenId);

      this.hasNext = q.step();
    }

    @Override
    public boolean tryAdvance(Consumer<? super Token> action) {
      if (!hasNext) return false;

      action.accept(q.getToken());
      hasNext = q.step();
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
  }

  /// Getters for source file data and methods to alter it's contents
  public class Source {
    private final int id;

    public Source(int id) {
      this.id = id;
    }

    public final int id() {
      return id;
    }

    public final Path path() {
      var q = new Query(sourcePath);
      q.set(id());

      q.stepNext();
      var result = Path.of(q.getString().orElseThrow());
      q.stepEnd();
      return result;
    }

    public final int packId() {
      var q = new Query(sourcePack);
      q.set(id());

      q.stepNext();
      int result = q.get();
      q.stepEnd();
      return result;
    }

    public final byte[] md5() {
      var q = new Query(sourceMd5);
      q.set(id());

      q.stepNext();
      byte[] result = q.getBlob().orElseThrow();
      q.stepEnd();
      return result;
    }

    public final int getNodeId() {
      var q = new Query(sourceNodeId);
      q.set(id());

      q.stepNext();
      int result = q.get();
      q.stepEnd();
      return result;
    }

    public final void setNodeId(Node<org.zenframework.z8.blls.ast.Source> node) {
      var q = new Query(sourceNodeIdSet);
      q.set(node.id());
      q.set(id());

      q.stepEnd();

      int packId = packId();

      {
        var e = new Extra.Source();
        e.packId = -packId;
        extraSet(node.id(), e);
      }

      node.data().children(Ast.this).forEach(it -> {
        int typeId = switch (it.data()) {
          case Klass v -> v.childId();
          case org.zenframework.z8.blls.ast.Enumeration _ -> it.id();
          default -> throw new AssertionError();
        };
        String name = switch (it.data()) {
          case Klass v -> v.name(Ast.this);
          case org.zenframework.z8.blls.ast.Enumeration v -> v.name(Ast.this);
          default -> throw new AssertionError();
        };

        var q2 = new Query(pack);
        q2.set(packId);
        q2.set(name);

        int childPackId = 0;
        int childNodeId = 0;
        if (q2.step()) {
          childPackId = q2.get();
          childNodeId = q2.get();
          q2.stepEnd();
        }

        if (childNodeId == 0) {
          if (childPackId < 0) {
            // TODO: pack/declaration clash diagnostic
          }

          q2 = new Query(packSetChild);
          q2.set(packId);
          q2.set(name);
          if (childPackId == 0)
            q2.setNull();
          else
            q2.set(childPackId);
          q2.set(typeId);

          q2.stepEnd();

          switch (it.data()) {
            case Klass _ -> {
              var e = new Extra.Declaration();
              e.packId = -packId;
              extraSet(typeId, e);
            }
            case org.zenframework.z8.blls.ast.Enumeration _ -> {
              var e = new Extra.Enumeration();
              e.packId = -packId;
              extraSet(typeId, e);
            }
            default -> throw new AssertionError();
          }
        } else {
          // TODO: declaration/declaration clash diagnostic
        }
      });
    }

    public final int getStep() {
      var q = new Query(sourceStep);
      q.set(id());

      q.stepNext();
      int result = q.get();
      q.stepEnd();
      return result;
    }

    public final void setStep(int value) {
      var q = new Query(sourceStepSet);
      q.set(value);
      q.set(id());

      q.stepEnd();
    }

    public final String getDiagnostic() {
      var q = new Query(sourceDiagnostic);
      q.set(id());

      q.stepNext();
      String result = q.getString().orElseThrow();
      q.stepEnd();
      return result;
    }

    public final void setDiagnostic(String value) {
      var q = new Query(sourceDiagnosticSet);
      q.set(value);
      q.set(id());

      q.stepEnd();
    }

    public final UUID getDiagnosticId() {
      var q = new Query(sourceDiagnosticId);
      q.set(id());

      q.stepNext();
      long least = q.getLong();
      long most = q.getLong();
      q.stepEnd();

      if (least == 0 && most == 0) {
        var result = UUID.randomUUID();
        q = new Query(sourceDiagnosticIdSet);
        q.set(result.getLeastSignificantBits());
        q.set(result.getMostSignificantBits());
        q.set(id());

        q.stepEnd();

        return result;
      } else {
        return new UUID(most, least);
      }
    }

    public final void clearDiagnostic() {
      var q = new Query(sourceDiagnosticClear);
      q.set(id());

      q.stepEnd();
    }

    public final void setDependency(int sourceId) {
      var q = new Query(sourceDependencySet);
      q.set(id());
      q.set(sourceId);

      q.stepEnd();
    }

    public final Stream<S> dependencies() {
      var q = new Query(sourceDependencies);
      q.set(id());

      q.stepNext();
      int size = q.get();

      return stream(new Spliterator<>() {
        private boolean hasNext = q.step();

        @Override
        public boolean tryAdvance(Consumer<? super S> action) {
          if (!hasNext) return false;

          action.accept(sourceOf(q.get()));
          hasNext = q.step();
          return true;
        }

        @Override
        public @Nullable Spliterator<S> trySplit() {
          return null;
        }

        @Override
        public long estimateSize() {
          return size;
        }

        @Override
        public int characteristics() {
          return DISTINCT | SIZED | NONNULL | IMMUTABLE;
        }
      }, false);
    }

    public final void delete() {
      var q = new Query(sourceDelete);
      q.set(id());

      q.stepEnd();
    }

    public final int stringSet(String value) {
      var q = new Query(stringSet);
      q.set(value);

      q.stepNext();
      int result = q.get();
      q.stepEnd();
      return result;
    }

    public final int tokenSet(Token value) {
      var q = new Query(tokenSet);
      q.set(id());
      q.set(value.kind().ordinal());
      q.set(value.line());
      q.set(value.character());
      q.set(value.index());

      q.stepNext();
      int result = q.get();
      q.stepEnd();
      return result;
    }

    public final int nodeSet(int nodeStartId, int nodeEndId, Node.Data data) {
      assert nodeStartId > 0;
      assert nodeEndId > 0;

      var q = new Query(nodeSet);
      q.set(id());
      q.set(nodeStartId);
      q.set(nodeEndId);
      q.set((switch (data) {
        case org.zenframework.z8.blls.ast.Source _ -> Kind.SOURCE;
        case Import _ -> Kind.IMPORT;
        case Klass _ -> Kind.KLASS;
        case org.zenframework.z8.blls.ast.Enumeration _ -> Kind.ENUMERATION;
        case Annotation _ -> Kind.ANNOTATION;
        case Parameter _ -> Kind.PARAMETER;
        case org.zenframework.z8.blls.ast.Declaration _ -> Kind.DECLARATION;
        case org.zenframework.z8.blls.ast.Declaration.Child v -> switch (v) {
          case org.zenframework.z8.blls.ast.Declaration.Field _ -> Kind.DECLARATION_FIELD;
          case org.zenframework.z8.blls.ast.Declaration.Initialize _ -> Kind.DECLARATION_INITIALIZE;
          case org.zenframework.z8.blls.ast.Declaration.Method _ -> Kind.DECLARATION_METHOD;
          case org.zenframework.z8.blls.ast.Declaration.Priority _ -> Kind.DECLARATION_PRIORITY;
          case org.zenframework.z8.blls.ast.Declaration.Cast _ -> Kind.DECLARATION_CAST;
          case org.zenframework.z8.blls.ast.Declaration.Operator _ -> Kind.DECLARATION_OPERATOR;
          case org.zenframework.z8.blls.ast.Declaration.Records _ -> Kind.DECLARATION_RECORDS;
        };
        case org.zenframework.z8.blls.ast.Declaration.Records.Child _ -> Kind.DECLARATION_RECORDS_CHILD;
        case org.zenframework.z8.blls.ast.Enumeration.Child _ -> Kind.ENUMERATION_CHILD;
        case Statement v -> switch (v) {
          case Statement.Block _ -> Kind.STATEMENT_BLOCK;
          case Statement.Expression _ -> Kind.STATEMENT_EXPRESSION;
          case Statement.Variable _ -> Kind.STATEMENT_VARIABLE;
          case Statement.Return _ -> Kind.STATEMENT_RETURN;
          case Statement.Break _ -> Kind.STATEMENT_BREAK;
          case Statement.Continue _ -> Kind.STATEMENT_CONTINUE;
          case Statement.Throw _ -> Kind.STATEMENT_THROW;
          case Statement.Try _ -> Kind.STATEMENT_TRY;
          case Statement.Condition _ -> Kind.STATEMENT_CONDITION;
          case Statement.LoopDo _ -> Kind.STATEMENT_LOOPDO;
          case Statement.LoopWhile _ -> Kind.STATEMENT_LOOPWHILE;
          case Statement.LoopFor _ -> Kind.STATEMENT_LOOPFOR;
          case Statement.LoopForEach _ -> Kind.STATEMENT_LOOPFOREACH;
        };
        case Statement.Try.Catch _ -> Kind.STATEMENT_TRY_CATCH;
        case Expression v -> switch (v) {
          case Expression.Paren _ -> Kind.EXPRESSION_PAREN;
          case Expression.Null _ -> Kind.EXPRESSION_NULL;
          case Expression.Boolean _ -> Kind.EXPRESSION_BOOLEAN;
          case Expression.Integer _ -> Kind.EXPRESSION_INTEGER;
          case Expression.Decimal _ -> Kind.EXPRESSION_DECIMAL;
          case Expression.String _ -> Kind.EXPRESSION_STRING;
          case Expression.Special _ -> Kind.EXPRESSION_SPECIAL;
          case Expression.This _ -> Kind.EXPRESSION_THIS;
          case Expression.Super _ -> Kind.EXPRESSION_SUPER;
          case Expression.Container _ -> Kind.EXPRESSION_CONTAINER;
          case Expression.New _ -> Kind.EXPRESSION_NEW;
          case Expression.Array _ -> Kind.EXPRESSION_ARRAY;
          case Expression.Access _ -> Kind.EXPRESSION_ACCESS;
          case Expression.Index _ -> Kind.EXPRESSION_INDEX;
          case Expression.Invoke _ -> Kind.EXPRESSION_INVOKE;
          case Expression.Cast _ -> Kind.EXPRESSION_CAST;
          case Expression.Object _ -> Kind.EXPRESSION_OBJECT;
          case Expression.Unary _ -> Kind.EXPRESSION_UNARY;
          case Expression.Binary _ -> Kind.EXPRESSION_BINARY;
          case Expression.Instanceof _ -> Kind.EXPRESSION_INSTANCEOF;
          case Expression.Ternary _ -> Kind.EXPRESSION_TERNARY;
        };
        case Expression.Array.Child.Value _ -> Kind.EXPRESSION_ARRAY_CHILD_VALUE;
        case Expression.Array.Child.Entry _ -> Kind.EXPRESSION_ARRAY_CHILD_ENTRY;
        case Expression.Invoke.Argument _ -> Kind.EXPRESSION_INVOKE_ARGUMENT;
        case Definition v -> switch (v) {
          case Definition.Reference _ -> Kind.DEFINITION_REFERENCE;
          case Definition.Array _ -> Kind.DEFINITION_ARRAY;
          case Definition.Map _ -> Kind.DEFINITION_MAP;
        };
      }).ordinal());
      q.set(json.writeValueAsString(data));

      q.stepNext();
      int result = q.get();
      q.stepEnd();
      return result;
    }

    public final void parentSet(int nodeId, int nodeParentId) {
      assert nodeId > 0;
      assert nodeParentId > 0;

      var q = new Query(parentSet);
      q.set(nodeParentId);
      q.set(nodeId);

      q.stepEnd();
    }

    public final void siblingSet(int nodeId, int nodeSiblingId) {
      assert nodeId > 0;
      assert nodeSiblingId > 0;

      var q = new Query(node);
      q.set(nodeId);

      q.stepNext();
      var _ = q.get();
      String data = q.getString().orElseThrow();
      q.stepEnd();

      try (var parser = json.createParser(data)) {
        q = new Query(dataSet);
        q.set(
          json.writer().writeValueAsString(
            parser.<ObjectNode>readValueAsTree().put("siblingId", nodeSiblingId)
          )
        );
        q.set(nodeId);

        q.stepEnd();
      }
    }
  }
}
