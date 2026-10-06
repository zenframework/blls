package org.zenframework.z8.blls.lsp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.analyzer.MapInt;
import org.zenframework.z8.blls.analyzer.Resolver;
import org.zenframework.z8.blls.ast.Annotation;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Node;
import org.zenframework.z8.blls.ast.Token;
import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.VisitorCheckSupported;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.json.JsonReadFeature;

import java.io.*;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.joining;
import static java.util.stream.StreamSupport.intStream;
import static org.zenframework.z8.blls.analyzer.Ast.only;

public final class LanguageServer implements org.eclipse.lsp4j.services.LanguageServer, LanguageClientAware, TextDocumentService, WorkspaceService {
  // Support json5 for .blls.json5
  public static final Set<JsonReadFeature> JACKSON_FEATURES = unmodifiableSet(EnumSet.of(
    JsonReadFeature.ALLOW_JAVA_COMMENTS,
    JsonReadFeature.ALLOW_SINGLE_QUOTES,
    JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES,
    JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS,
    JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
    JsonReadFeature.ALLOW_HEXADECIMAL_NUMBERS,
    JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS,
    JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS,
    JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS,
    JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS,
    JsonReadFeature.ALLOW_TRAILING_COMMA
  ));
  private static final String CLIENT_ECLIPSE = "Eclipse IDE";
  private static final String CONFIGURATION_DATABASE_VENDOR = "databaseVendor";
  private static final String CONFIGURATION_INLAY_HINT_BOOLEAN = "inlayHintBoolean";
  private static final String CONFIGURATION_INLAY_HINT_NON_OBVIOUS = "inlayHintNonObvious";
  private static final String CONFIGURATION_INLAY_HINT_IMPLICIT = "inlayHintImplicit";
  private static final String REQUEST_REGISTER_CONFIGURATION = "blls_register_configuration";
  private static final String REQUEST_REGISTER_WATCHED_FILES = "blls_register_watched_files";
  private static final int WALK_LIMIT = 25;
  private final org.zenframework.z8.blls.astmemory.Ast ast;
  private final @Nullable Path trace;
  private final @Nullable Path z8;
  private final MapInt<Integer> versions = new MapInt<>();
  private final MapInt<?>.Keys dirty = new MapInt<>().keySet();
  private final Map<String, Path> modules = new HashMap<>();
  @SuppressWarnings("DataFlowIssue")
  private LanguageClient client = null;
  private Level level = Level.OFF;
  private DatabaseVendor databaseVendor = DatabaseVendor.Postgres;
  private boolean inlayHintBoolean = false;
  private boolean inlayHintNonObvious = false;
  private boolean inlayHintImplicit = false;
  private Map<String, Expression.Type.Declaration> primaries = Map.of();

  public LanguageServer(
    org.zenframework.z8.blls.astmemory.Ast ast,
    @Nullable Path trace,
    @Nullable Path z8
  ) {
    this.ast = ast;
    this.trace = trace;
    this.z8 = z8;
  }

  public static boolean isBlFile(Path path) {
    return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) &&
           // no dot files
           !path.getFileName().toString().startsWith(".") &&
           path.getFileName().toString().endsWith(".bl");
  }

  private static Duration measure(Runnable action) {
    long start = currentTimeMillis();
    action.run();
    long end = currentTimeMillis();
    return Duration.ofMillis(end - start);
  }

  private static ConfigurationItem configurationItemOf(String configuration) {
    var result = new ConfigurationItem();
    result.setSection("blls." + configuration);
    return result;
  }

  private void log(Supplier<String> message) {
    log(message, null);
  }

  private void log(Supplier<String> message, @Nullable Supplier<String> verbose) {
    if (level.compareTo(Level.INFO) <= 0) {
      var params = new LogTraceParams(message.get());
      if (level.compareTo(Level.TRACE) <= 0 && verbose != null)
        params.setVerbose(verbose.get());
      client.logTrace(params);
    }
  }

  private void logException(String message, Throwable e) {
    log(() -> message, () -> {
      var sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      return sw.toString();
    });
  }

  private void logSourcesDirty() {
    log(
      () -> "sources: dirty: " + sourcesDirty().count(),
      () -> sourcesDirty().map(this::uri).collect(joining("\n"))
    );
  }

  private void logSourceDependencies(org.zenframework.z8.blls.astmemory.Ast.Source source) {
    log(
      () -> "%s: %d dependencies; %d step2; %d step3; %d step4; %d step5".formatted(
        uri(source),
        source.dependencies().count(),
        source.dependencies().filter(it -> it.getStep() == 2).count(),
        source.dependencies().filter(it -> it.getStep() == 3).count(),
        source.dependencies().filter(it -> it.getStep() == 4).count(),
        source.dependencies().filter(it -> it.getStep() == 5).count()
      ),
      () -> source.dependencies().map(this::uri).collect(joining("\n"))
    );
  }

  private void logTime(String prefix, Duration duration) {
    log(() -> prefix + ": " + duration);
  }

  private Optional<Path> pathCanonicalOf(String uri) {
    var path = Path.of(URI.create(uri));
    return modules.entrySet()
      .stream()
      .filter(it -> path.startsWith(it.getValue()))
      .findFirst()
      .map(it -> Path.of(it.getKey()).resolve(it.getValue().relativize(path)));
  }

  private Optional<org.zenframework.z8.blls.astmemory.Ast.Source> sourceOfPath(Path path) {
    return ast.sources()
      .filter(it -> it.path().equals(path))
      .findFirst();
  }

  private Stream<org.zenframework.z8.blls.astmemory.Ast.Source> sourcesDirty() {
    return intStream(dirty.spliterator(), false)
      .mapToObj(it -> ast.sourceOf((short) it));
  }

  private void sourceCollectDirty(org.zenframework.z8.blls.astmemory.Ast.Source source) {
    // setStep(0) in case parse() failed
    source.setStep(Math.min(source.getStep(), 1));
    source.setDependencies(new int[0]);
    source.clearDiagnostic();
    source.clearNodes();
    var _ = ast.sources()
      .filter(it -> binarySearch(it.getDependencies(), source.id()) >= 0)
      .peek(this::sourceCollectDirty)
      .collect(() -> dirty, (downstream, it) -> downstream.add(it.id()), MapInt.Keys::addAll);
  }

  private Ast ast() {
    return new Ast(ast) {
      @Override
      protected String uriOf(Node<?> node) {
        return uri(ast.sourceOf(node)) + "#L" + (start(node).line() + 1);
      }
    };
  }

  private boolean setDatabaseVendor(@Nullable JsonElement value) {
    if (value instanceof JsonPrimitive v && v.isString()) {
      try {
        var newValue = DatabaseVendor.valueOf(v.getAsString());
        boolean result = databaseVendor != newValue;
        databaseVendor = newValue;
        return result;
      } catch (IllegalArgumentException _) {
        databaseVendor = DatabaseVendor.Postgres;
        return false;
      }
    } else return false;
  }

  private boolean setInlayHintBoolean(@Nullable JsonElement value) {
    if (value instanceof JsonPrimitive v && v.isBoolean()) {
      boolean newValue = v.getAsBoolean();
      boolean result = inlayHintBoolean != newValue;
      inlayHintBoolean = newValue;
      return result;
    } else return false;
  }

  private boolean setInlayHintNonObvious(@Nullable JsonElement value) {
    if (value instanceof JsonPrimitive v && v.isBoolean()) {
      boolean newValue = v.getAsBoolean();
      boolean result = inlayHintNonObvious != newValue;
      inlayHintNonObvious = newValue;
      return result;
    } else return false;
  }

  private boolean setInlayHintImplicit(@Nullable JsonElement value) {
    if (value instanceof JsonPrimitive v && v.isBoolean()) {
      boolean newValue = v.getAsBoolean();
      boolean result = inlayHintImplicit != newValue;
      inlayHintImplicit = newValue;
      return result;
    } else return false;
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    var project = params.getClientInfo().getName().equals(CLIENT_ECLIPSE)
      ? Path.of(URI.create(params.getRootUri()))
      : Path.of(URI.create(params.getWorkspaceFolders().getFirst().getUri()));

    DotBlls dotBlls = null;
    try {
      dotBlls = ast.json().readValue(project.resolve(".blls.json5"), DotBlls.class);
    } catch (JacksonIOException _) {
    } catch (Exception e) {
      logException(".blls.json5: " + e.getMessage(), e);
    }

    if (dotBlls == null) {
      Path v;
      if (
        Files.exists((v = project.resolve("org.zenframework.z8.lang/src/bl")), LinkOption.NOFOLLOW_LINKS) ||
        Files.exists((v = project.resolve("src/main/bl")), LinkOption.NOFOLLOW_LINKS) ||
        Files.exists((v = project.resolve("src/bl")), LinkOption.NOFOLLOW_LINKS) ||
        Files.exists((v = project.resolve("bl")), LinkOption.NOFOLLOW_LINKS)
      ) {
        modules.put("main", v);
      } else {
        client.showMessage(new MessageParams(
          MessageType.Warning,
          "Не удалось распознать структуру проекта. Укажите модули в файле .blls.json5"
        ));
      }
      if (z8 != null)
        modules.put("z8", z8);
    } else {
      if (dotBlls.z8 && z8 != null)
        modules.put("z8", z8);

      dotBlls.modules.forEach((name, path) -> modules.put(name, project.resolve(path).normalize()));
    }

    switch (params.getTrace()) {
      case TraceValue.Messages -> level = Level.INFO;
      case TraceValue.Verbose -> level = Level.TRACE;
      case null, default -> {
      }
    }

    if (params.getInitializationOptions() instanceof JsonObject v) try {
      setDatabaseVendor(v.get(CONFIGURATION_DATABASE_VENDOR));
      setInlayHintBoolean(v.get(CONFIGURATION_INLAY_HINT_BOOLEAN));
      setInlayHintNonObvious(v.get(CONFIGURATION_INLAY_HINT_NON_OBVIOUS));
      setInlayHintImplicit(v.get(CONFIGURATION_INLAY_HINT_IMPLICIT));
    } catch (Exception _) {
    }

    var sc = new ServerCapabilities();
    sc.setPositionEncoding(PositionEncodingKind.UTF16);
    var tdso = new TextDocumentSyncOptions();
    tdso.setOpenClose(true);
    tdso.setChange(TextDocumentSyncKind.Full);
    sc.setTextDocumentSync(tdso);
    sc.setHoverProvider(true);
    sc.setDefinitionProvider(true);
    sc.setTypeDefinitionProvider(true);
    sc.setImplementationProvider(true);
    sc.setReferencesProvider(true);
    sc.setDocumentHighlightProvider(true);
    sc.setDocumentSymbolProvider(true);
    sc.setCodeLensProvider(new CodeLensOptions());
    sc.setInlayHintProvider(true);

    return completedFuture(
      new InitializeResult(
        sc,
        new ServerInfo(
          "blls",
          getClass().getModule().getDescriptor().rawVersion().orElseThrow()
        )
      )
    );
  }

  @Override
  public void initialized(InitializedParams params) {
    if (trace != null) {
      client.showMessage(new MessageParams(MessageType.Info, "Запись логов: " + trace));
    }

    client.registerCapability(new RegistrationParams(List.of(
      new Registration(REQUEST_REGISTER_CONFIGURATION, "workspace/didChangeConfiguration"),
      new Registration(
        REQUEST_REGISTER_WATCHED_FILES,
        "workspace/didChangeWatchedFiles",
        new DidChangeWatchedFilesRegistrationOptions(List.of(
          new FileSystemWatcher(
            Either.forLeft("**/*.bl"),
            WatchKind.Create | WatchKind.Delete
          )
        ))
      )
    )));

    var z8 = modules.get("z8");
    if (z8 != null) {
      Path v;
      if (
        Files.exists((v = z8.resolve("org.zenframework.z8.lang/src/bl")), LinkOption.NOFOLLOW_LINKS) ||
        Files.exists((v = z8.resolve("src/main/bl")), LinkOption.NOFOLLOW_LINKS) ||
        Files.exists((v = z8.resolve("src/bl")), LinkOption.NOFOLLOW_LINKS) ||
        Files.exists((v = z8.resolve("bl")), LinkOption.NOFOLLOW_LINKS)
      ) {
        modules.put("z8", v);
      } else if (!z8.endsWith("bl")) {
        client.showMessage(new MessageParams(MessageType.Warning, "Ошибка импорта z8: путь не распознан: " + z8));
        modules.remove("z8");
      }
    }

    {
      var entries = modules.entrySet().stream().toList();
      modules.clear();

      for (var e : entries) {
        if (modules.values().stream().noneMatch(it -> e.getValue().startsWith(it))) {
          modules.put(e.getKey(), e.getValue());
        }
      }
    }

    for (var e : modules.entrySet()) {
      try (var blFiles = Files.walk(e.getValue(), WALK_LIMIT).filter(LanguageServer::isBlFile)) {
        blFiles.forEach(it -> {
          try (var reader = Files.newBufferedReader(it)) {
            parse(ast.sourceOf(Path.of(e.getKey()).resolve(e.getValue().relativize(it))), reader);
          } catch (IOException _) {
          }
        });
      } catch (IOException _) {
      }
    }

    primaries = ast().analyzer().primaries();

    logTime("visitor1 (all)", measure(() -> ast.sources().forEach(this::visitor1)));
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    return completedFuture(null);
  }

  @Override
  public void exit() {
  }

  @Override
  public void setTrace(SetTraceParams params) {
    level = switch (params.getValue()) {
      case TraceValue.Off -> Level.OFF;
      case TraceValue.Messages -> Level.INFO;
      case TraceValue.Verbose -> Level.TRACE;
      default -> throw new AssertionError();
    };
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return this;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return this;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) {
      client.logMessage(new MessageParams(
        MessageType.Warning,
        "Out of scope: %s".formatted(params.getTextDocument().getUri())
      ));
      return;
    }

    sourceOfPath(path).ifPresentOrElse(
      source -> {
        versions.put(source.id(), Integer.valueOf(params.getTextDocument().getVersion()));
        logSourceDependencies(source);
        logTime("visitor2,3,4,checkSupported", measure(() -> {
          source.dependencies().forEach(this::visitor2);
          source.dependencies().forEach(this::visitor3);
          source.dependencies().forEach(this::visitor4);
          ast().evalsql().ifPresent(ast -> source.dependencies().forEach(it -> visitorCheckSupported(it, ast)));
        }));

        versions.forEach((sourceId, sourceVersion) -> {
          var s = ast.sourceOf(sourceId.shortValue());
          client.publishDiagnostics(new PublishDiagnosticsParams(uri(s), diagnostic(s), sourceVersion));
        });
      },
      () -> {
        var source = ast.sourceOf(path);
        versions.put(source.id(), Integer.valueOf(params.getTextDocument().getVersion()));

        logTime("parse: " + path, measure(() -> {
          try {
            parse(source, new StringReader(params.getTextDocument().getText()));
          } catch (IOException _) {
            throw new AssertionError();
          }
        }));

        primaries = ast().analyzer().primaries();

        logTime("visitor1", measure(() -> visitor1(source)));

        logSourceDependencies(source);
        logTime("visitor2,3,4,checkSupported", measure(() -> {
          source.dependencies().forEach(this::visitor2);
          source.dependencies().forEach(this::visitor3);
          source.dependencies().forEach(this::visitor4);
          ast().evalsql().ifPresent(ast -> source.dependencies().forEach(it -> visitorCheckSupported(it, ast)));
        }));

        versions.forEach((sourceId, sourceVersion) -> {
          var s = ast.sourceOf(sourceId.shortValue());
          client.publishDiagnostics(new PublishDiagnosticsParams(uri(s), diagnostic(s), sourceVersion));
        });
      }
    );
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return;

    {
      var source = sourceOfPath(path).orElseThrow();
      logTime("delete: " + path, measure(() -> {
        sourceCollectDirty(source);
        source.delete();
      }));
    }

    var source = ast.sourceOf(path);
    versions.put(source.id(), params.getTextDocument().getVersion());

    logTime("parse: " + path, measure(() -> {
      try {
        parse(source, new StringReader(params.getContentChanges().getFirst().getText()));
      } catch (IOException _) {
        throw new AssertionError();
      }
    }));
    dirty.add(source.id());

    primaries = ast().analyzer().primaries();

    logSourcesDirty();
    logTime("visitor1", measure(() -> sourcesDirty().forEach(this::visitor1)));
    dirty.clear();

    logSourceDependencies(source);
    logTime("visitor2,3,4,checkSupported", measure(() -> {
      source.dependencies().forEach(this::visitor2);
      source.dependencies().forEach(this::visitor3);
      source.dependencies().forEach(this::visitor4);
      ast().evalsql().ifPresent(ast -> source.dependencies().forEach(it -> visitorCheckSupported(it, ast)));
    }));

    versions.forEach((sourceId, sourceVersion) -> {
      var s = ast.sourceOf(sourceId.shortValue());
      client.publishDiagnostics(new PublishDiagnosticsParams(uri(s), diagnostic(s), sourceVersion));
    });
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return;

    versions.remove(sourceOfPath(path).orElseThrow().id());
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
  }

  @Override
  public CompletableFuture<@Nullable Hover> hover(HoverParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    try {
      var v = new VisitorHover() {
        @Nullable Hover result = null;

        @Override
        protected Ast ast() {
          return LanguageServer.this.ast();
        }

        @Override
        protected Position position() {
          return params.getPosition();
        }

        @Override
        protected void success(Hover value) {
          result = value;
        }
      };
      v.visit(source.node().id());
      return completedFuture(v.result);
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/hover: ошибка выполнения"
      ));
      logException("textDocument/hover: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<@Nullable Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    try {
      var v = new VisitorDefinition() {
        @Nullable Location result = null;

        @Override
        protected Ast ast() {
          return LanguageServer.this.ast();
        }

        @Override
        protected Position position() {
          return params.getPosition();
        }

        @Override
        protected void success(Node<?> node) {
          result = new Location(
            uri(ast.sourceOf(node)),
            ast().anchor(node)
          );
        }
      };
      v.visit(source.node().id());
      return completedFuture(v.result == null ? null : Either.forLeft(List.of(v.result)));
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/definition: ошибка выполнения"
      ));
      logException("textDocument/definition: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<@Nullable Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(TypeDefinitionParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    try {
      var v = new VisitorTypeDefinition() {
        @Nullable Location result = null;

        @Override
        protected Ast ast() {
          return LanguageServer.this.ast();
        }

        @Override
        protected Position position() {
          return params.getPosition();
        }

        @Override
        protected void successDeclaration(Node<org.zenframework.z8.blls.ast.Declaration> node) {
          result = new Location(
            uri(ast.sourceOf(node)),
            ast().anchor(node)
          );
        }

        @Override
        protected void successEnumeration(Node<org.zenframework.z8.blls.ast.Enumeration> node) {
          result = new Location(
            uri(ast.sourceOf(node)),
            ast().anchor(node)
          );
        }
      };
      v.visit(source.node().id());
      return completedFuture(v.result == null ? null : Either.forLeft(List.of(v.result)));
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/typeDefinition: ошибка выполнения"
      ));
      logException("textDocument/typeDefinition: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> implementation(ImplementationParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    logTime("visitor2,3", measure(() -> {
      ast.sources().forEach(this::visitor2);
      ast.sources().forEach(this::visitor3);
    }));

    try {
      var v = new VisitorImplementation() {
        final ArrayList<Location> result = new ArrayList<>();

        @Override
        protected Ast ast() {
          return LanguageServer.this.ast();
        }

        @Override
        protected Position position() {
          return params.getPosition();
        }

        @Override
        protected void success(Node<?> node) {
          result.add(new Location(
            uri(ast.sourceOf(node)),
            ast().anchor(node)
          ));
        }

        @Override
        protected void visitGlobal(VisitorGlobal visitor) {
          ast.sources()
            .filter(it -> it.getStep() > 0)
            .forEach(it -> visitor.visit(it.node().id()));
        }
      };
      v.visit(source.node().id());
      return completedFuture(Either.forLeft(v.result));
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/implementation: ошибка выполнения"
      ));
      logException("textDocument/implementation: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    logTime("visitor2,3", measure(() -> {
      ast.sources().forEach(this::visitor2);
      ast.sources().forEach(this::visitor3);
    }));

    try {
      var v = new VisitorReferences() {
        final ArrayList<Location> result = new ArrayList<>();

        @Override
        protected Ast ast() {
          return LanguageServer.this.ast();
        }

        @Override
        protected Position position() {
          return params.getPosition();
        }

        @Override
        protected void success(Node<?> node) {
          result.add(new Location(
            uri(ast.sourceOf(node)),
            ast().anchor(node)
          ));
        }

        @Override
        protected void visitGlobal(VisitorGlobal visitor) {
          ast.sources()
            .filter(it -> it.getStep() > 0)
            .forEach(it -> visitor.visit(it.node().id()));
        }
      };
      v.visit(source.node().id());
      return completedFuture(v.result);
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/references: ошибка выполнения"
      ));
      logException("textDocument/references: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    try {
      var v = new VisitorDocumentHighlight() {
        final ArrayList<DocumentHighlight> result = new ArrayList<>();

        @Override
        protected Ast ast() {
          return LanguageServer.this.ast();
        }

        @Override
        protected Position position() {
          return params.getPosition();
        }

        @Override
        protected Node<org.zenframework.z8.blls.ast.Source> source() {
          return source.node();
        }

        @Override
        protected void success(DocumentHighlight value) {
          result.add(value);
        }
      };
      v.visit(source.node().id());
      return completedFuture(v.result);
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/documentHighlight: ошибка выполнения"
      ));
      logException("textDocument/documentHighlight: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<@Nullable List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    try {
      var v = new VisitorDocumentSymbol() {
        @Nullable List<DocumentSymbol> result = null;

        @Override
        protected Ast ast() {
          return LanguageServer.this.ast();
        }

        @Override
        protected void success(List<DocumentSymbol> value) {
          result = value;
        }
      };
      v.visit(source.node().id());
      return completedFuture(v.result == null ? null : v.result.stream().<Either<SymbolInformation, DocumentSymbol>>map(Either::forRight).toList());
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/documentSymbol: ошибка выполнения"
      ));
      logException("textDocument/documentSymbol: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<@Nullable List<? extends CodeLens>> codeLens(CodeLensParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    var $Field = Resolver.runSimple(ast, List.of("org", "zenframework", "z8", "base", "table", "value", "Field").iterator())
      .stream()
      .gather(only(org.zenframework.z8.blls.ast.Declaration.class))
      .findFirst()
      .orElse(null);
    if ($Field == null)
      return completedFuture(null);

    var astEvalsql = ast().evalsql().orElse(null);
    if (astEvalsql == null)
      return completedFuture(null);

    try {
      var v = new VisitorCodeLens() {
        final ArrayList<CodeLens> result = new ArrayList<>();

        @Override
        protected Ast astLsp() {
          return LanguageServer.this.ast();
        }

        @Override
        protected org.zenframework.z8.blls.evalsql.Ast ast() {
          return astEvalsql;
        }

        @Override
        protected DatabaseVendor vendor() {
          return databaseVendor;
        }

        @Override
        protected void success(CodeLens value) {
          result.add(value);
        }

        @Override
        protected Expression.Type.Declaration typeOf(String name) {
          assert name.equals("Field");
          return new Expression.Type.Declaration($Field.id());
        }
      };
      v.visit(source.node().id());
      return completedFuture(v.result);
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/codeLens: ошибка выполнения"
      ));
      logException("textDocument/codeLens: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
    var path = pathCanonicalOf(params.getTextDocument().getUri()).orElse(null);
    if (path == null) return completedFuture(null);

    var source = sourceOfPath(path).orElseThrow();
    if (source.getStep() == 0) return completedFuture(null);

    try {
      var v = new VisitorInlayHint() {
        final ArrayList<InlayHint> result = new ArrayList<>();

        @Override
        protected void success(InlayHint value) {
          result.add(value);
        }

        @Override
        protected Optional<Expression.Type.Declaration> primaryOf(String name) {
          return Optional.ofNullable(primaries.get(name));
        }

        @Override
        protected Location locationOf(Node<?> node) {
          return new Location(
            uri(ast.sourceOf(node)),
            ast().anchor(node)
          );
        }

        @Override
        protected Range range() {
          return params.getRange();
        }

        @Override
        protected boolean inlayHintBoolean() {
          return inlayHintBoolean;
        }

        @Override
        protected boolean inlayHintNonObvious() {
          return inlayHintNonObvious;
        }

        @Override
        protected boolean inlayHintImplicit() {
          return inlayHintImplicit;
        }

        @Override
        protected Ast ast() {
          return LanguageServer.this.ast();
        }
      };
      v.visit(source.node().id());
      return completedFuture(v.result);
    } catch (Throwable e) {
      client.showMessage(new MessageParams(
        MessageType.Warning,
        "textDocument/inlayHint: ошибка выполнения"
      ));
      logException("textDocument/inlayHint: %s: %s".formatted(source.path(), e.getMessage()), e);
      return completedFuture(null);
    }
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    if (params.getSettings() instanceof JsonObject v) {
      if (
        setDatabaseVendor(v.get(CONFIGURATION_DATABASE_VENDOR) instanceof JsonElement v2 ? v2 : null)
      ) {
        client.refreshCodeLenses();
      }

      if (
        setInlayHintBoolean(v.get(CONFIGURATION_INLAY_HINT_BOOLEAN)) ||
        setInlayHintNonObvious(v.get(CONFIGURATION_INLAY_HINT_NON_OBVIOUS)) ||
        setInlayHintImplicit(v.get(CONFIGURATION_INLAY_HINT_IMPLICIT))
      ) {
        client.refreshInlayHints();
      }
    } else {
      client.configuration(
        new ConfigurationParams(
          List.of(
            configurationItemOf(CONFIGURATION_DATABASE_VENDOR),
            configurationItemOf(CONFIGURATION_INLAY_HINT_BOOLEAN),
            configurationItemOf(CONFIGURATION_INLAY_HINT_NON_OBVIOUS),
            configurationItemOf(CONFIGURATION_INLAY_HINT_IMPLICIT)
          )
        )
      ).whenCompleteAsync((result, _) -> {
        if (
          setDatabaseVendor(result.get(0) instanceof JsonElement v ? v : null)
        ) {
          client.refreshCodeLenses();
        }

        if (
          setInlayHintBoolean(result.get(1) instanceof JsonElement v ? v : null) ||
          setInlayHintNonObvious(result.get(2) instanceof JsonElement v ? v : null) ||
          setInlayHintImplicit(result.get(3) instanceof JsonElement v ? v : null)
        ) {
          client.refreshInlayHints();
        }
      });
    }
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    params.getChanges().forEach(event -> {
      switch (event.getType()) {
        case FileChangeType.Created -> {
          var path = Path.of(URI.create(event.getUri()));
          if (!isBlFile(path)) return;

          var pathCanonical = pathCanonicalOf(event.getUri()).orElse(null);
          if (pathCanonical == null) return;

          try (var reader = Files.newBufferedReader(path)) {
            var s = ast.sourceOf(pathCanonical);
            parse(s, reader);
            dirty.add(s.id());
          } catch (IOException _) {
          }
        }
        case FileChangeType.Deleted -> pathCanonicalOf(event.getUri())
          .flatMap(this::sourceOfPath)
          .ifPresent(it -> {
            sourceCollectDirty(it);
            it.delete();
          });
      }
    });

    primaries = ast().analyzer().primaries();

    logSourcesDirty();
    logTime("visitor1", measure(() -> sourcesDirty().forEach(this::visitor1)));
    dirty.clear();
  }

  private String uri(org.zenframework.z8.blls.astmemory.Ast.Source source) {
    var path = source.path();
    return modules.get(path.getName(0).toString()).resolve(path.subpath(1, path.getNameCount())).toUri().toASCIIString();
  }

  @SuppressWarnings("unchecked")
  private List<Diagnostic> diagnostic(org.zenframework.z8.blls.astmemory.Ast.Source source) {
    return (List<Diagnostic>) source.getDiagnostic();
  }

  private void diagnosticOf(
    org.zenframework.z8.blls.astmemory.Ast.Source source,
    Range range,
    String message,
    DiagnosticSeverity severity,
    Node<?>... related
  ) {
    var d = new Diagnostic(range, message, severity, "blls");
    if (related.length > 0) {
      d.setRelatedInformation(
        stream(related)
          .map(it -> new DiagnosticRelatedInformation(
            new Location(
              uri(ast.sourceOf(it)),
              ast().anchor(it)
            ),
            "Объявлено здесь"
          ))
          .toList()
      );
    }
    source.setDiagnostic(d);
  }

  private void parse(org.zenframework.z8.blls.astmemory.Ast.Source source, Reader reader) throws IOException {
    try {
      var lexer = new org.zenframework.z8.blls.compiler.Lexer(reader) {
        private @Nullable Token last;

        @Override
        protected int stringSet(String value) {
          return ast.stringSet(value);
        }

        @Override
        protected int tokenSet(Token value) {
          last = value;
          return source.tokenSet(value);
        }

        @Override
        public void yyerror(org.zenframework.z8.blls.compiler.Parser.Location loc, String msg) {
          if (last == null)
            diagnosticOf(
              source,
              new Range(new Position(0, 0), new Position(0, 0)),
              msg,
              DiagnosticSeverity.Error
            );
          else
            diagnosticOf(
              source,
              ast().range(last, last),
              msg,
              DiagnosticSeverity.Error
            );
        }
      };

      var parser = new org.zenframework.z8.blls.compiler.Parser(lexer) {
        @Override
        protected org.zenframework.z8.blls.ast.Ast ast() {
          return ast;
        }

        @Override
        protected void sourceSet(org.zenframework.z8.blls.ast.Source result) {
          source.initialize(
            new Node<>(
              source.nodeSet(
                ast().tokensBackWhitespace(lexer.getEndPos())
                  .reduce((_, b) -> b)
                  .orElseThrow()
                  .id(),
                lexer.getEndPos(),
                result
              ),
              result
            )
          );
          source.setStep(1);
        }

        @Override
        protected int nodeSet(org.zenframework.z8.blls.compiler.Parser.Location location, Node.Data data) {
          return source.nodeSet(location.begin, location.end, data);
        }

        @Override
        protected void siblingSet(int nodeId, int nodeSiblingId) {
          ast.siblingSet(nodeId, nodeSiblingId);
        }
      };

      if (parser.parse()) {
        new org.zenframework.z8.blls.ast.Visitor() {
          private int parentId = source.node().id();

          @Override
          protected org.zenframework.z8.blls.ast.Ast ast() {
            return ast;
          }

          @Override
          public void visit(int id) {
            int parentId = this.parentId;
            ast.parentSet(id, parentId);
            this.parentId = id;
            super.visit(id);
            this.parentId = parentId;
          }

          @Override
          protected void source(Node<org.zenframework.z8.blls.ast.Source> node) {
            super.source(node);
          }
        }.source(source.node());
      }
    } catch (Throwable e) {
      logException("parse: %s: %s".formatted(source.path(), e.getMessage()), e);
    }
  }

  private void visitor1(org.zenframework.z8.blls.astmemory.Ast.Source source) {
    if (source.getStep() == 1) try {
      var visitor1 = ast().new Visitor1() {
        @Override
        protected void errorOf(Range range, String message, Node<?>... related) {
          diagnosticOf(source, range, message, DiagnosticSeverity.Error, related);
        }

        @Override
        protected Optional<Expression.Type.Declaration> primaryOf(String name) {
          return Optional.ofNullable(primaries.get(name));
        }
      };
      visitor1.visit(source.node().id());
      source.setDependencies(
        visitor1.typeDependencies
          .stream()
          .map(ast::node)
          .map(ast::sourceOf)
          .mapToInt(org.zenframework.z8.blls.astmemory.Ast.Source::id)
          .distinct()
          .sorted()
          .toArray()
      );
    } catch (Throwable e) {
      logException("visitor1: %s: %s".formatted(source.path(), e.getMessage()), e);
    } finally {
      source.setStep(2);
    }
  }

  private void visitor2(org.zenframework.z8.blls.astmemory.Ast.Source source) {
    if (source.getStep() == 2) try {
      ast().new Visitor2() {
        @Override
        protected void warningOf(Range range, String message, Node<?>... related) {
          diagnosticOf(source, range, message, DiagnosticSeverity.Warning, related);
        }

        @Override
        protected void errorOf(Range range, String message, Node<?>... related) {
          diagnosticOf(source, range, message, DiagnosticSeverity.Error, related);
        }

        @Override
        protected Optional<Expression.Type.Declaration> primaryOf(String name) {
          return Optional.ofNullable(primaries.get(name));
        }

        @Override
        protected void typeSet(Node<? extends Expression> node, Expression.Type value) {
          ast.typeSet(node, value);
        }
      }.visit(source.node().id());
    } catch (Throwable e) {
      logException("visitor2: %s: %s".formatted(source.path(), e.getMessage()), e);
    } finally {
      source.setStep(3);
    }
  }

  private void visitor3(org.zenframework.z8.blls.astmemory.Ast.Source source) {
    if (source.getStep() == 3) try {
      ast().new Visitor3() {
        @Override
        protected void warningOf(Range range, String message, Node<?>... related) {
          diagnosticOf(source, range, message, DiagnosticSeverity.Warning, related);
        }

        @Override
        protected void errorOf(Range range, String message, Node<?>... related) {
          diagnosticOf(source, range, message, DiagnosticSeverity.Error, related);
        }

        @Override
        protected Optional<Expression.Type.Declaration> primaryOf(String name) {
          return Optional.ofNullable(primaries.get(name));
        }
      }.visit(source.node().id());
    } catch (Throwable e) {
      logException("visitor3: %s: %s".formatted(source.path(), e.getMessage()), e);
    } finally {
      source.setStep(4);
    }
  }

  private void visitor4(org.zenframework.z8.blls.astmemory.Ast.Source source) {
    if (source.getStep() == 4) try {
      ast().new Visitor4() {
        @Override
        protected void warningOf(Range range, String message, Node<?>... related) {
          diagnosticOf(source, range, message, DiagnosticSeverity.Warning, related);
        }

        @Override
        protected void informationOf(Range range, String message, Node<?>... related) {
          diagnosticOf(source, range, message, DiagnosticSeverity.Information, related);
        }

        @Override
        protected void diagnosticAnnotation(Node<Annotation> node, Node<Annotation> shadows) {
          var d = new Diagnostic(
            LanguageServer.this.ast().anchor(node),
            "Аннотация применена в родительском классе",
            DiagnosticSeverity.Information,
            "blls"
          );
          d.setRelatedInformation(List.of(
            new DiagnosticRelatedInformation(
              new Location(
                uri(ast.sourceOf(node)),
                LanguageServer.this.ast().anchor(node)
              ),
              "Объявлено здесь"
            )
          ));
          d.setTags(List.of(DiagnosticTag.Unnecessary));
          source.setDiagnostic(d);
        }
      }.visit(source.node().id());
    } catch (Throwable e) {
      logException("visitor4: %s: %s".formatted(source.path(), e.getMessage()), e);
    } finally {
      source.setStep(5);
    }
  }

  private void visitorCheckSupported(org.zenframework.z8.blls.astmemory.Ast.Source source, org.zenframework.z8.blls.evalsql.Ast ast) {
    if (source.getStep() == 5) try {
      new VisitorCheckSupported() {
        @Override
        protected org.zenframework.z8.blls.evalsql.Ast ast() {
          return ast;
        }

        @Override
        protected DatabaseVendor vendor() {
          return databaseVendor;
        }

        @Override
        protected void diagnosticDatabaseUnsupported(SqlToken token) {
          int relatedId = switch (ast.data(token.nodeId())) {
            case Expression.Invoke v -> ast.extra(token.nodeId(), Extra.ExpressionInvoke.class).resolvedId;
            case Expression.Unary v -> ast.extra(token.nodeId(), Extra.ExpressionUnary.class).resolvedId;
            case Expression.Binary v -> ast.extra(token.nodeId(), Extra.ExpressionBinary.class).resolvedId;
            default -> 0;
          };
          diagnosticOf(
            source,
            LanguageServer.this.ast().anchor(ast.node(token.nodeId())),
            "%s: операция не поддерживается в %s".formatted(token.getClass().getSimpleName(), vendor()),
            DiagnosticSeverity.Warning,
            relatedId == 0 ? new Node[0] : new Node[]{ast.node(relatedId)}
          );
        }
      }.visit(source.node().id());
    } catch (Throwable e) {
      logException("visitor5: %s: %s".formatted(source.path(), e.getMessage()), e);
    } finally {
      source.setStep(6);
    }
  }

  private static final class DotBlls {
    @SuppressWarnings("FieldMayBeFinal")
    public boolean z8 = true;
    @SuppressWarnings("FieldMayBeFinal")
    public Map<String, Path> modules = Map.of();
  }
}
