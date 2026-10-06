package org.zenframework.z8.blls;

import org.eclipse.lsp4j.jsonrpc.*;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.jsonrpc.validation.ReflectiveMessageValidator;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.*;
import org.zenframework.z8.blls.ast.*;
import org.zenframework.z8.blls.ast.Ast;
import org.zenframework.z8.blls.ast.Enumeration;
import org.zenframework.z8.blls.lsp.LanguageServer;
import picocli.CommandLine;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.System.*;
import static java.util.stream.Collectors.toMap;

@CommandLine.Command(
  name = "blls",
  description = "Языковой сервер для BL"
)
public final class Main implements Callable<Integer> {

  @CommandLine.Option(
    names = "--sqlite",
    description = "Путь к библиотеке SQLite",
    paramLabel = "<файл>"
  )
  private @Nullable Path sqlite;
  @CommandLine.Option(
    names = "--z8",
    description = "Путь к z8",
    paramLabel = "<папка>"
  )
  private @Nullable Path z8;
  @CommandLine.Option(
    names = "--record",
    description = "Записывать запросы",
    defaultValue = "false"
  )
  private boolean record;
  @CommandLine.Option(
    names = "--input",
    description = "Читать запросы из файла, а не ввода",
    paramLabel = "<файл>"
  )
  private @Nullable Path input;
  @CommandLine.Option(
    names = "--validate-lsp4j",
    description = "Включить проверки в lsp4j",
    defaultValue = "false"
  )
  private boolean validateLsp4j;

  static void main(String[] args) {
    System.exit(new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true).execute(args));
  }

  @Override
  public Integer call() throws IOException {
    var recordPath = record ? Files.createTempFile("blls-", null) : null;
    try (var recordWriter = recordPath == null ? null : new PrintWriter(Files.newBufferedWriter(recordPath))) {
      var supportedMethods = new LinkedHashMap<>(ServiceEndpoints.getSupportedMethods(LanguageClient.class));
      supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(LanguageServer.class));

      var jsonHandler = new MessageJsonHandler(supportedMethods);

      var sentRequests = new HashMap<String, TracingMessageConsumer.RequestMetadata>();
      var receivedRequests = new HashMap<String, TracingMessageConsumer.RequestMetadata>();

      MessageConsumer messageConsumer = new StreamMessageConsumer(out, jsonHandler);
      if (recordWriter != null) {
        var tmc = new TracingMessageConsumer(
          messageConsumer,
          sentRequests,
          receivedRequests,
          recordWriter,
          Clock.systemDefaultZone()
        );
        tmc.setJsonHandler(jsonHandler);
        messageConsumer = tmc;
      }
      if (validateLsp4j) {
        messageConsumer = new ReflectiveMessageValidator(messageConsumer);
      }

      var blls = new LanguageServer(
        new org.zenframework.z8.blls.astmemory.Ast(
          JsonMapper.builder()
            .enable(org.zenframework.z8.blls.astmemory.Ast.JACKSON_FEATURES.toArray(EnumFeature[]::new))
            .addModule(org.zenframework.z8.blls.astmemory.Ast.JACKSON_MODULE)
            .enable(LanguageServer.JACKSON_FEATURES.toArray(JsonReadFeature[]::new))
            .build()
        ),
        recordPath,
        z8
      );

      var remoteEndpoint = new RemoteEndpoint(messageConsumer, ServiceEndpoints.toEndpoint(blls));
      jsonHandler.setMethodProvider(remoteEndpoint);
      remoteEndpoint.setJsonHandler(jsonHandler);
      var remoteProxy = ServiceEndpoints.toServiceObject(remoteEndpoint, LanguageClient.class);

      var messageProducer = input == null
        ? new StreamMessageProducer(in, jsonHandler, remoteEndpoint)
        : new MessageProducerOfTraces();

      messageConsumer = remoteEndpoint;
      if (recordWriter != null) {
        var tmc = new TracingMessageConsumer(
          messageConsumer,
          sentRequests,
          receivedRequests,
          recordWriter,
          Clock.systemDefaultZone()
        );
        tmc.setJsonHandler(jsonHandler);
        messageConsumer = tmc;
      }
      if (validateLsp4j) {
        messageConsumer = new ReflectiveMessageValidator(messageConsumer);
      }

      blls.connect(remoteProxy);

      messageProducer.listen(messageConsumer);
    }

    return 0;
  }

  @CommandLine.Command
  public int ast(
    @CommandLine.Option(
      names = "--tokens",
      description = "Выводить токены",
      negatable = true,
      defaultValue = "false"
    )
    boolean tokens,
    @CommandLine.Parameters(
      paramLabel = "<путь>",
      description = "Путь к .bl файлу"
    )
    Path input
  ) throws IOException {
    var json = JsonMapper.builder()
      .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
      .enable(org.zenframework.z8.blls.astmemory.Ast.JACKSON_FEATURES.toArray(EnumFeature[]::new))
      .addModule(org.zenframework.z8.blls.astmemory.Ast.JACKSON_MODULE)
      .build();
    var astMemory = new org.zenframework.z8.blls.astmemory.Ast(json);
    try (
      var reader = Files.newBufferedReader(input);
      var generator = json.writerWithDefaultPrettyPrinter().createGenerator(out)
    ) {
      var astMemorySource = astMemory.sourceOf(input);

      var lexer = new org.zenframework.z8.blls.compiler.Lexer(reader) {
        @Override
        protected int stringSet(String value) {
          return astMemory.stringSet(value);
        }

        @Override
        protected int tokenSet(Token value) {
          return astMemorySource.tokenSet(value);
        }

        @Override
        public void yyerror(org.zenframework.z8.blls.compiler.Parser.Location loc, String msg) {
          err.println("lex error " + msg);
        }
      };

      var parser = new org.zenframework.z8.blls.compiler.Parser(lexer) {
        @Override
        protected Ast ast() {
          return astMemory;
        }

        @Override
        protected void sourceSet(org.zenframework.z8.blls.ast.Source result) {
          astMemorySource.initialize(
            new Node<>(
              astMemorySource.nodeSet(
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
        }

        @Override
        protected int nodeSet(org.zenframework.z8.blls.compiler.Parser.Location location, Node.Data data) {
          return astMemorySource.nodeSet(location.begin, location.end, data);
        }

        @Override
        protected void siblingSet(int nodeId, int nodeSiblingId) {
          astMemory.siblingSet(nodeId, nodeSiblingId);
        }
      };

      if (!parser.parse()) {
        err.println("parse error");
        return 1;
      }

      new org.zenframework.z8.blls.ast.Visitor() {
        private int parentId = astMemorySource.node().id();

        @Override
        protected Ast ast() {
          return astMemory;
        }

        @Override
        public void visit(int id) {
          int parentId = this.parentId;
          astMemory.parentSet(id, parentId);
          this.parentId = id;
          super.visit(id);
          this.parentId = parentId;
        }

        @Override
        protected void source(Node<org.zenframework.z8.blls.ast.Source> node) {
          super.source(node);
        }
      }.source(astMemorySource.node());

      new org.zenframework.z8.blls.ast.Visitor() {
        @Override
        protected org.zenframework.z8.blls.ast.Ast ast() {
          return astMemory;
        }

        private void visit(String property, int id) {
          generator.writeName(property);
          if (id == 0) generator.writeNull();
          else visit(id);
        }

        private void visitSiblings(String property, int ids) {
          generator.writeArrayPropertyStart(property);
          ast().siblings(ids).forEach(this::visit);
          generator.writeEndArray();
        }

        private void token(Token token) {
          generator.writeStartObject();
          generator.writeNumberProperty("id", token.id());
          generator.writeStringProperty("kind", token.kind().name());
          generator.writeNumberProperty("line", token.line());
          generator.writeNumberProperty("character", token.character());
          generator.writeNumberProperty("index", token.index());
          generator.writeEndObject();
        }

        @Override
        public void visit(int id) {
          var n = ast().node(id);
          generator.writeStartObject();
          generator.writeNumberProperty("__id", id);
          generator.writeStringProperty("__class", n.data().getClass().getCanonicalName());
          if (tokens) {
            generator.writeName("__start");
            token(ast().start(n));
            generator.writeName("__end");
            token(ast().end(n));
          }
          super.visit(id);
          generator.writeEndObject();
        }

        @Override
        protected void source(Node<Source> node) {
          visitSiblings("imports", node.data().importsId());
          visitSiblings("children", node.data().childrenId());
        }

        @Override
        protected void import_(Node<Import> node) {
          visit("value", node.data().valueId());
        }

        @Override
        protected void klass(Node<Klass> node) {
          visitSiblings("annotations", node.data().annotationsId());
          generator.writeNumberProperty("modifiers", node.data().modifiers());
          generator.writeStringProperty("name", node.data().name(ast()));
          visit("extended", node.data().extendedId());
          visit("child", node.data().childId());
        }

        @Override
        protected void enumeration(Node<Enumeration> node) {
          visitSiblings("annotations", node.data().annotationsId());
          generator.writeNumberProperty("modifiers", node.data().modifiers());
          generator.writeStringProperty("name", node.data().name(ast()));
          visitSiblings("children", node.data().childrenId());
        }

        @Override
        protected void annotation(Node<Annotation> node) {
          generator.writeStringProperty("name", node.data().name(ast()));
          visit("value", node.data().valueId());
        }

        @Override
        protected void parameter(Node<Parameter> node) {
          visit("definition", node.data().definitionId());
          generator.writeStringProperty("name", node.data().name(ast()));
        }

        @Override
        protected void declaration(Node<Declaration> node) {
          visitSiblings("children", node.data().childrenId());
        }

        @Override
        protected void declarationField(Node<Declaration.Field> node) {
          visitSiblings("annotations", node.data().annotationsId());
          generator.writeNumberProperty("modifiers", node.data().modifiers());
          visit("definition", node.data().definitionId());
          generator.writeStringProperty("name", node.data().name(ast()));
          visit("value", node.data().valueId());
        }

        @Override
        protected void declarationInitialize(Node<Declaration.Initialize> node) {
          visitSiblings("annotations", node.data().annotationsId());
          visit("child", node.data().childId());
        }

        @Override
        protected void declarationMethod(Node<Declaration.Method> node) {
          visitSiblings("annotations", node.data().annotationsId());
          generator.writeNumberProperty("modifiers", node.data().modifiers());
          visit("definition", node.data().definitionId());
          generator.writeStringProperty("name", node.data().name(ast()));
          visitSiblings("parameters", node.data().parametersId());
          visit("block", node.data().blockId());
        }

        @Override
        protected void declarationPriority(Node<Declaration.Priority> node) {
          visitSiblings("annotations", node.data().annotationsId());
          generator.writeNumberProperty("modifiers", node.data().modifiers());
          visitSiblings("parameters", node.data().parametersId());
          visit("block", node.data().blockId());
        }

        @Override
        protected void declarationCast(Node<Declaration.Cast> node) {
          visitSiblings("annotations", node.data().annotationsId());
          generator.writeNumberProperty("modifiers", node.data().modifiers());
          visit("definition", node.data().definitionId());
          visitSiblings("parameters", node.data().parametersId());
          visit("block", node.data().blockId());
        }

        @Override
        protected void declarationOperator(Node<Declaration.Operator> node) {
          visitSiblings("annotations", node.data().annotationsId());
          generator.writeNumberProperty("modifiers", node.data().modifiers());
          visit("definition", node.data().definitionId());
          generator.writeStringProperty("operation", node.data().operation().name());
          visitSiblings("parameters", node.data().parametersId());
          visit("block", node.data().blockId());
        }

        @Override
        protected void declarationRecords(Node<Declaration.Records> node) {
          visitSiblings("children", node.data().childrenId());
        }

        @Override
        protected void declarationRecordsChild(Node<Declaration.Records.Child> node) {
          visitSiblings("annotations", node.data().annotationsId());
          visit("value", node.data().valueId());
        }

        @Override
        protected void enumerationChild(Node<Enumeration.Child> node) {
          generator.writeStringProperty("name", node.data().name(ast()));
        }

        @Override
        protected void statementBlock(Node<Statement.Block> node) {
          visitSiblings("statements", node.data().statementsId());
        }

        @Override
        protected void statementExpression(Node<Statement.Expression> node) {
          visit("value", node.data().valueId());
        }

        @Override
        protected void statementVariable(Node<Statement.Variable> node) {
          visit("definition", node.data().definitionId());
          generator.writeStringProperty("name", node.data().name(ast()));
          visit("value", node.data().valueId());
        }

        @Override
        protected void statementReturn(Node<Statement.Return> node) {
          visit("value", node.data().valueId());
        }

        @Override
        protected void statementBreak(Node<Statement.Break> node) {
          super.statementBreak(node);
        }

        @Override
        protected void statementContinue(Node<Statement.Continue> node) {
          super.statementContinue(node);
        }

        @Override
        protected void statementThrow(Node<Statement.Throw> node) {
          visit("value", node.data().valueId());
        }

        @Override
        protected void statementTry(Node<Statement.Try> node) {
          visit("block", node.data().blockId());
          visitSiblings("catches", node.data().catchesId());
          visit("blockFinally", node.data().blockFinallyId());
        }

        @Override
        protected void statementCondition(Node<Statement.Condition> node) {
          visit("condition", node.data().conditionId());
          visit("then", node.data().thenId());
          visit("other", node.data().otherId());
        }

        @Override
        protected void statementLoopWhile(Node<Statement.LoopWhile> node) {
          visit("condition", node.data().conditionId());
          visit("statement", node.data().statementId());
        }

        @Override
        protected void statementLoopFor(Node<Statement.LoopFor> node) {
          visit("initializer", node.data().initializerId());
          visit("condition", node.data().conditionId());
          visit("update", node.data().updateId());
          visit("statement", node.data().statementId());
        }

        @Override
        protected void statementLoopForEach(Node<Statement.LoopForEach> node) {
          visit("definition", node.data().definitionId());
          generator.writeStringProperty("name", node.data().name(ast()));
          visit("expression", node.data().expressionId());
          visit("statement", node.data().statementId());
        }

        @Override
        protected void statementTryCatch(Node<Statement.Try.Catch> node) {
          visit("definition", node.data().definitionId());
          generator.writeStringProperty("name", node.data().name(ast()));
          visit("block", node.data().blockId());
        }

        @Override
        protected void expressionParen(Node<Expression.Paren> node) {
          visit("child", node.data().childId());
        }

        @Override
        protected void expressionNull(Node<Expression.Null> node) {
          super.expressionNull(node);
        }

        @Override
        protected void expressionBoolean(Node<Expression.Boolean> node) {
          super.expressionBoolean(node);
        }

        @Override
        protected void expressionInteger(Node<Expression.Integer> node) {
          super.expressionInteger(node);
        }

        @Override
        protected void expressionDecimal(Node<Expression.Decimal> node) {
          super.expressionDecimal(node);
        }

        @Override
        protected void expressionString(Node<Expression.String> node) {
          super.expressionString(node);
        }

        @Override
        protected void expressionSpecial(Node<Expression.Special> node) {
          super.expressionSpecial(node);
        }

        @Override
        protected void expressionThis(Node<Expression.This> node) {
          super.expressionThis(node);
        }

        @Override
        protected void expressionSuper(Node<Expression.Super> node) {
          super.expressionSuper(node);
        }

        @Override
        protected void expressionContainer(Node<Expression.Container> node) {
          visit("child", node.data().childId());
        }

        @Override
        protected void expressionNew(Node<Expression.New> node) {
          visit("definition", node.data().definitionId());
        }

        @Override
        protected void expressionArray(Node<Expression.Array> node) {
          visit("definition", node.data().definitionId());
          visitSiblings("children", node.data().childrenId());
        }

        @Override
        protected void expressionAccess(Node<Expression.Access> node) {
          visit("child", node.data().childId());
          generator.writeStringProperty("name", node.data().name(ast()));
        }

        @Override
        protected void expressionIndex(Node<Expression.Index> node) {
          visit("child", node.data().childId());
          visit("value", node.data().valueId());
        }

        @Override
        protected void expressionInvoke(Node<Expression.Invoke> node) {
          visit("child", node.data().childId());
          generator.writeStringProperty("name", node.data().name(ast()));
          visitSiblings("arguments", node.data().argumentsId());
        }

        @Override
        protected void expressionCast(Node<Expression.Cast> node) {
          visit("definition", node.data().definitionId());
          visit("value", node.data().valueId());
        }

        @Override
        protected void expressionObject(Node<Expression.Object> node) {
          visit("child", node.data().childId());
        }

        @Override
        protected void expressionUnary(Node<Expression.Unary> node) {
          generator.writeStringProperty("operation", node.data().operation().name());
          visit("value", node.data().valueId());
        }

        @Override
        protected void expressionBinary(Node<Expression.Binary> node) {
          visit("left", node.data().leftId());
          generator.writeStringProperty("operation", node.data().operation().name());
          visit("right", node.data().rightId());
        }

        @Override
        protected void expressionInstanceof(Node<Expression.Instanceof> node) {
          visit("value", node.data().valueId());
          visit("definition", node.data().definitionId());
        }

        @Override
        protected void expressionTernary(Node<Expression.Ternary> node) {
          visit("condition", node.data().conditionId());
          visit("then", node.data().thenId());
          visit("other", node.data().otherId());
        }

        @Override
        protected void expressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
          visit("value", node.data().valueId());
        }

        @Override
        protected void expressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
          visit("key", node.data().keyId());
          visit("value", node.data().valueId());
        }

        @Override
        protected void expressionInvokeArgument(Node<Expression.Invoke.Argument> node) {
          visit("value", node.data().valueId());
        }

        @Override
        protected void definitionReference(Node<Definition.Reference> node) {
          visit("child", node.data().childId());
          generator.writeStringProperty("name", node.data().name(ast()));
        }

        @Override
        protected void definitionArray(Node<Definition.Array> node) {
          visit("value", node.data().valueId());
        }

        @Override
        protected void definitionMap(Node<Definition.Map> node) {
          visit("value", node.data().valueId());
          visit("key", node.data().keyId());
        }
      }.visit(astMemorySource.node().id());

      out.println();

      return 0;
    }
  }

  @CommandLine.Command
  public int analyze(
    @CommandLine.Option(
      names = "--warnings",
      negatable = true,
      defaultValue = "true",
      fallbackValue = "true"
    )
    boolean warnings,
    @CommandLine.Parameters(
      description = "Корневые каталоги"
    )
    Map<String, Path> modules
  ) {
    if (z8 != null)
      modules.put("z8", z8);

    var astMemory = new org.zenframework.z8.blls.astmemory.Ast(
      JsonMapper.builder()
        .enable(org.zenframework.z8.blls.astmemory.Ast.JACKSON_FEATURES.toArray(EnumFeature[]::new))
        .addModule(org.zenframework.z8.blls.astmemory.Ast.JACKSON_MODULE)
        .build()
    );

    var sources = modules.entrySet()
      .stream()
      .flatMap(module -> {
        try (var blFiles = Files.walk(module.getValue(), 25).filter(LanguageServer::isBlFile)) {
          return blFiles.map(it -> {
            try (var reader = Files.newBufferedReader(it)) {
              var astMemorySource = astMemory.sourceOf(Path.of(module.getKey()).resolve(module.getValue().relativize(it)));
              return Map.entry(new Analyze() {
                @Override
                protected Ast ast() {
                  return astMemory;
                }

                @Override
                protected Path path() {
                  return it;
                }

                @Override
                protected int stringSet(String value) {
                  return astMemory.stringSet(value);
                }

                @Override
                protected int tokenSet(Token value) {
                  return astMemorySource.tokenSet(value);
                }

                @Override
                protected int nodeSet(int nodeStartId, int nodeEndId, Node.Data data) {
                  return astMemorySource.nodeSet(nodeStartId, nodeEndId, data);
                }

                @Override
                protected void parentSet(int nodeId, int nodeParentId) {
                  astMemory.parentSet(nodeId, nodeParentId);
                }

                @Override
                protected void siblingSet(int nodeId, int nodeSiblingId) {
                  astMemory.siblingSet(nodeId, nodeSiblingId);
                }

                @Override
                protected void sourceSet(Node<Source> source) {
                  astMemorySource.initialize(source);
                }
              }.run(reader), astMemorySource.path());
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          }).toList().stream();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      })
      .collect(toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (_, _) -> {
          throw new AssertionError();
        },
        MapInt<Path>::new
      ));

    analyze(astMemory, sources, astMemory::typeSet, warnings);

    return 0;
  }

  private void analyze(Ast ast, MapInt<Path> sources, BiConsumer<Node<? extends Expression>, Expression.Type> typeSet, boolean warnings) {
    var primaries = new org.zenframework.z8.blls.analyzer.Ast(ast).primaries();

    for (var source : sources.entrySet()) {
      new Visitor1() {
        @Override
        protected org.zenframework.z8.blls.analyzer.Ast ast() {
          return new org.zenframework.z8.blls.analyzer.Ast(ast);
        }

        @Override
        protected Optional<Expression.Type.Declaration> primaryOf(String name) {
          return Optional.ofNullable(primaries.get(name));
        }

        @Override
        protected void diagnosticDefinitionReferenceAbsent(Node<Definition.Reference> node, Ast.Pack pack) {
          err.printf("visitor1: %s: diagnosticDefinitionReferenceAbsent\n", source.getValue());
        }

        @Override
        protected void diagnosticDefinitionReferenceExcess(Node<Definition.Reference> node, Node<?> result) {
          err.printf("visitor1: %s: diagnosticDefinitionReferenceExcess\n", source.getValue());
        }

        @Override
        protected void diagnosticDefinitionReferenceLack(Node<Definition.Reference> node, Ast.Pack pack) {
          err.printf("visitor1: %s: diagnosticDefinitionReferenceLack\n", source.getValue());
        }
      }.visit(source.getKey());
    }

    for (var source : sources.entrySet()) {
      new Visitor2() {
        @Override
        protected org.zenframework.z8.blls.analyzer.Ast ast() {
          return new org.zenframework.z8.blls.analyzer.Ast(ast);
        }

        @Override
        protected Optional<Expression.Type.Declaration> primaryOf(String name) {
          return Optional.ofNullable(primaries.get(name));
        }

        @Override
        protected void typeSet(Node<? extends Expression> node, Expression.Type value) {
          typeSet.accept(node, value);
        }

        @Override
        protected void diagnosticAbsent(Node<?> node, Expression.Type source2, Operation operation) {
          err.printf("visitor2: %s: diagnosticAbsent\n", source.getValue());
        }

        @Override
        protected void diagnosticAmbiguous(Node<?> node, Stream<? extends Node<? extends Declaration.Child>> candidates) {
          err.printf("visitor2: %s#%d: diagnosticAmbiguous\n", source.getValue(), ast().start(node).line());
        }

        @Override
        protected void diagnosticWrong(Node<?> node, Expression.Type type) {
          err.printf("visitor2: %s: diagnosticWrong\n", source.getValue());
        }

        @Override
        protected void diagnosticInstance(Node<?> node) {
          err.printf("visitor2: %s: diagnosticInstance\n", source.getValue());
        }

        @Override
        protected void diagnosticType(Node<?> node, Expression.Type target, Expression.Type source2) {
          err.printf("visitor2: %s: diagnosticType\n", source.getValue());
        }

        @Override
        protected void diagnosticTypeNoHint(Node<?> node) {
          err.printf("visitor2: %s: diagnosticTypeNoHint\n", source.getValue());
        }

        @Override
        protected void diagnosticStatementReturnNothing(Node<?> node) {
          err.printf("visitor2: %s: diagnosticStatementReturnNothing\n", source.getValue());
        }

        @Override
        protected void diagnosticStatementReturnSomething(Node<?> node, Expression.Type target) {
          err.printf("visitor2: %s: diagnosticStatementReturnSomething\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionSpecial(Node<Expression.Special> node) {
          err.printf("visitor2: %s: diagnosticExpressionSpecial\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionSuper(Node<Expression.Super> node) {
          err.printf("visitor2: %s: diagnosticExpressionSuper\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionContainer(Node<Expression.Container> node) {
          err.printf("visitor2: %s: diagnosticExpressionContainer\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionArray(Node<Expression.Array> node) {
          err.printf("visitor2: %s: diagnosticExpressionArray\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
          err.printf("visitor2: %s: diagnosticExpressionArrayChildValue\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
          err.printf("visitor2: %s: diagnosticExpressionArrayChildEntry\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionAccessAbsent(Node<Expression.Access> node, Expression.Type type) {
          err.printf("visitor2: %s: diagnosticExpressionAccessAbsent\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionAccessFailed(Node<Expression.Access> node) {
          err.printf("visitor2: %s#%d: diagnosticExpressionAccessFailed %s\n", source.getValue(), ast().start(node).line(), node.data().name(ast()));
        }

        @Override
        protected void diagnosticExpressionAccessStaticAbsent(Node<Expression.Access> node, Ast.Pack pack) {
          err.printf("visitor2: %s: diagnosticExpressionAccessStaticAbsent\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionAccessStaticAbsentDeclaration(Node<Expression.Access> node, Node<Declaration> declaration) {
          err.printf("visitor2: %s: diagnosticExpressionAccessStaticAbsentDeclaration\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionAccessStaticAbsentEnumeration(Node<Expression.Access> node, Node<Enumeration> enumeration) {
          err.printf("visitor2: %s#%d: diagnosticExpressionAccessStaticAbsentEnumeration %s %s\n", source.getValue(), ast().start(node).line(), node.data().name(ast()), enumeration.data().name(ast()));
        }

        @Override
        protected void diagnosticExpressionAccessStaticLack(Node<Expression.Access> node, Ast.PackChild result) {
          err.printf("visitor2: %s: diagnosticExpressionAccessStaticLack\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionIndex(Node<Expression.Index> node) {
          err.printf("visitor2: %s: diagnosticExpressionIndex\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionInvokeAbsent(Node<Expression.Invoke> node, Expression.Type type) {
          err.printf("visitor2: %s: diagnosticExpressionInvokeAbsent\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionInvokeAmbiguousSynthetic(Node<Expression.Invoke> node, Stream<org.zenframework.z8.blls.analyzer.Synthetic.Method> candidates) {
          err.printf("visitor2: %s: diagnosticExpressionInvokeAmbiguousSynthetic\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionInvokeStaticAbsentDeclaration(Node<Expression.Invoke> node, Node<Declaration> declaration) {
          err.printf("visitor2: %s: diagnosticExpressionInvokeStaticAbsentDeclaration\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionInvokeStaticAbsentEnumeration(Node<Expression.Invoke> node, Node<Enumeration> enumeration) {
          err.printf("visitor2: %s: diagnosticExpressionInvokeStaticAbsentEnumeration\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionCastSuccess(Node<Expression.Cast> node, Expression.Type source2, Expression.Type target) {
          if (warnings)
            err.printf("visitor2: %s: diagnosticExpressionCastSuccess\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionCastFail(Node<Expression.Cast> node, Expression.Type source2, Expression.Type target) {
          if (warnings)
            err.printf("visitor2: %s: diagnosticExpressionCastFail\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionInstanceofSuccess(Node<Expression.Instanceof> node) {
          if (warnings)
            err.printf("visitor2: %s: diagnosticExpressionInstanceofSuccess\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionInstanceofFail(Node<Expression.Instanceof> node, Expression.Type source2, Expression.Type target) {
          if (warnings)
            err.printf("visitor2: %s: diagnosticExpressionInstanceofFail\n", source.getValue());
        }
      }.visit(source.getKey());
    }

    for (var source : sources.entrySet()) {
      new Visitor3() {
        @Override
        protected org.zenframework.z8.blls.analyzer.Ast ast() {
          return new org.zenframework.z8.blls.analyzer.Ast(ast);
        }

        @Override
        protected Optional<Expression.Type.Declaration> primaryOf(String name) {
          return Optional.ofNullable(primaries.get(name));
        }

        @Override
        protected void diagnosticDuplicate(Node<?> nodeOld, Node<?> nodeNew) {
          err.printf("visitor3: %s: diagnosticDuplicate\n", source.getValue());
        }

        @Override
        protected void diagnosticType(Node<?> node, Expression.Type target, Expression.Type source2) {
          err.printf("visitor3: %s: diagnosticType\n", source.getValue());
        }

        @Override
        protected void diagnosticModifiers(Node<?> node, int modifiers) {
          err.printf("visitor3: %s: diagnosticModifiers\n", source.getValue());
        }

        @Override
        protected void diagnosticAnnotationUnknown(Node<Annotation> node) {
          if (warnings)
            err.printf("visitor3: %s: diagnosticAnnotationUnknown\n", source.getValue());
        }

        @Override
        protected void diagnosticAnnotationExpectNone(Node<Annotation> node) {
          err.printf("visitor3: %s: diagnosticAnnotationExpectNone\n", source.getValue());
        }

        @Override
        protected void diagnosticAnnotationExpectBoolean(Node<Annotation> node) {
          err.printf("visitor3: %s: diagnosticAnnotationExpectBoolean\n", source.getValue());
        }

        @Override
        protected void diagnosticAnnotationExpectString(Node<Annotation> node) {
          err.printf("visitor3: %s#%d: diagnosticAnnotationExpectString %s\n", source.getValue(), ast().start(node).line(), node.data().name(ast()));
        }

        @Override
        protected void diagnosticAnnotationExpectString(Node<Annotation> node, Pattern regex) {
          err.printf("visitor3: %s#%d: diagnosticAnnotationExpectString %s\n", source.getValue(), ast().start(node).line(), node.data().name(ast()));
        }

        @Override
        protected void diagnosticAnnotationExpectCron(Node<Annotation> node) {
          err.printf("visitor3: %s: diagnosticAnnotationExpectCron\n", source.getValue());
        }

        @Override
        protected void diagnosticParameters(Node<? extends Declaration.Child> node) {
          err.printf("visitor3: %s: diagnosticParameters\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationExtendsFinal(Node<Declaration> node, Node<Klass> extended) {
          err.printf("visitor3: %s: diagnosticDeclarationExtendsFinal\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationExtendsEnumeration(Node<Declaration> node, Node<Enumeration> extended) {
          err.printf("visitor3: %s: diagnosticDeclarationExtendsEnumeration\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationExtendsTypeWrong(Node<Declaration> node, Expression.Type type) {
          err.printf("visitor3: %s: diagnosticDeclarationExtendsTypeWrong\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationExtendsCycle(Stream<Node<Klass>> cycle) {
          err.printf("visitor3: %s: diagnosticDeclarationExtendsCycle\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationPrimaryConditions(Node<Declaration> node, Node<Klass> primary) {
          err.printf("visitor3: %s: diagnosticDeclarationPrimaryConditions\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationChildOverride(Node<? extends Declaration.Child> node, Node<? extends Declaration.Child> extended) {
          err.printf("visitor3: %s: diagnosticDeclarationChildOverride\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationChildReturns(Node<? extends Declaration.Child> node) {
          err.printf("visitor3: %s: diagnosticDeclarationChildReturns\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationChildUnimplemented(Node<? extends Declaration.Child> node) {
          err.printf("visitor3: %s: diagnosticDeclarationChildUnimplemented\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationFieldModifierAuto(Node<Declaration.Field> node) {
          err.printf("visitor3: %s: diagnosticDeclarationFieldModifierAuto\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationFieldModifierStatic(Node<Declaration.Field> node) {
          err.printf("visitor3: %s: diagnosticDeclarationFieldModifierStatic\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationFieldModifierFinal(Node<Declaration.Field> node) {
          err.printf("visitor3: %s: diagnosticDeclarationFieldModifierFinal\n", source.getValue());
        }

        @Override
        protected void diagnosticDeclarationInitializeAnnotation(Node<Declaration.Initialize> node) {
          err.printf("visitor3: %s: diagnosticDeclarationInitializeAnnotation\n", source.getValue());
        }

        @Override
        protected void diagnosticStatementVariable(Node<Statement.Variable> node, Node<Statement.Variable> shadowedBy) {
          err.printf("visitor3: %s: diagnosticStatementVariable\n", source.getValue());
        }

        @Override
        protected void diagnosticStatementBreak(Node<Statement.Break> node) {
          err.printf("visitor3: %s: diagnosticStatementBreak\n", source.getValue());
        }

        @Override
        protected void diagnosticStatementContinue(Node<Statement.Continue> node) {
          err.printf("visitor3: %s: diagnosticStatementContinue\n", source.getValue());
        }

        @Override
        protected void diagnosticStatementUnreachable(Node<? extends Statement> node) {
          err.printf("visitor3: %s: diagnosticStatementUnreachable\n", source.getValue());
        }

        @Override
        protected void diagnosticStatementTryCatchUnreachable(Node<Statement.Try.Catch> node, Node<Statement.Try.Catch> shadowedBy) {
          err.printf("visitor3: %s: diagnosticStatementTryCatchUnreachable\n", source.getValue());
        }

        @Override
        protected void diagnosticExpressionObject(Node<Expression.Object> node) {
          err.printf("visitor3: %s: diagnosticExpressionObject\n", source.getValue());
        }
      }.visit(source.getKey());
    }

    if (!warnings) return;

    for (var source : sources.entrySet()) {
      new Visitor4() {
        @Override
        protected org.zenframework.z8.blls.analyzer.Ast ast() {
          return new org.zenframework.z8.blls.analyzer.Ast(ast);
        }

        @Override
        protected void diagnosticImportUnused(Node<Import> node) {
          err.printf("visitor4: %s: diagnosticImportUnused\n", source.getValue());
        }

        @Override
        protected void diagnosticDeprecated(Node<?> node, Node<?> target, Node<Annotation> annotationDeprecated) {
          err.printf("visitor4: %s: diagnosticDeprecated\n", source.getValue());
        }

        @Override
        protected void diagnosticStatic(Node<?> node, Node<?> target) {
          err.printf("visitor4: %s: diagnosticStatic\n", source.getValue());
        }

        @Override
        protected void diagnosticImplicitCast(Node<?> node, Node<Declaration.Cast> cast) {
          err.printf("visitor4: %s: diagnosticImplicitCast\n", source.getValue());
        }

        @Override
        protected void diagnosticDuplicate(Node<Declaration.Initialize> nodeOld, Node<Declaration.Initialize> nodeNew) {
          err.printf("visitor4: %s: diagnosticDuplicate\n", source.getValue());
        }

        @Override
        protected void diagnosticAnnotation(Node<Annotation> node, Node<Annotation> shadows) {
          err.printf("visitor4: %s: diagnosticAnnotation\n", source.getValue());
        }
      }.visit(source.getKey());
    }
  }

  @CommandLine.Command
  public int version() {
    out.println(
      getClass().getModule()
        .getDescriptor()
        .rawVersion()
        .orElseThrow()
    );
    return 0;
  }

  private static final class MessageProducerOfTraces implements MessageProducer {
    @Override
    public void listen(MessageConsumer messageConsumer) {
      throw new UnsupportedOperationException();
    }
  }

  private static abstract class Analyze {
    protected abstract Ast ast();

    protected abstract Path path();

    protected abstract int stringSet(String value);

    protected abstract int tokenSet(Token value);

    protected abstract int nodeSet(int nodeStartId, int nodeEndId, Node.Data data);

    protected abstract void parentSet(int nodeId, int nodeParentId);

    protected abstract void siblingSet(int nodeId, int nodeSiblingId);

    protected abstract void sourceSet(Node<org.zenframework.z8.blls.ast.Source> source);

    protected final int run(Reader reader) throws IOException {
      var lexer = new org.zenframework.z8.blls.compiler.Lexer(reader) {
        private @Nullable Token last;

        @Override
        protected int stringSet(String value) {
          return Analyze.this.stringSet(value);
        }

        @Override
        protected int tokenSet(Token value) {
          last = value;
          return Analyze.this.tokenSet(value);
        }

        @Override
        public void yyerror(org.zenframework.z8.blls.compiler.Parser.Location loc, String msg) {
          if (last == null)
            err.printf("lexer: %s %s\n", path(), msg);
          else
            err.printf("lexer: %s#%d:%d %s\n".formatted(
              path(),
              last.line(),
              last.character(),
              msg
            ));
        }
      };

      var parser = new org.zenframework.z8.blls.compiler.Parser(lexer) {
        int nodeId;

        @Override
        protected Ast ast() {
          return Analyze.this.ast();
        }

        @Override
        protected void sourceSet(org.zenframework.z8.blls.ast.Source result) {
          nodeId = Analyze.this.nodeSet(
            ast().tokensBackWhitespace(lexer.getEndPos())
              .reduce((_, b) -> b)
              .orElseThrow()
              .id(),
            lexer.getEndPos(),
            result
          );
          Analyze.this.sourceSet(new Node<>(nodeId, result));
        }

        @Override
        protected int nodeSet(org.zenframework.z8.blls.compiler.Parser.Location location, Node.Data data) {
          return Analyze.this.nodeSet(location.begin, location.end, data);
        }

        @Override
        protected void siblingSet(int nodeId, int nodeSiblingId) {
          Analyze.this.siblingSet(nodeId, nodeSiblingId);
        }
      };

      if (!parser.parse())
        throw new RuntimeException("parse error");

      new org.zenframework.z8.blls.ast.Visitor() {
        private int parentId = parser.nodeId;

        @Override
        protected Ast ast() {
          return Analyze.this.ast();
        }

        @Override
        public void visit(int id) {
          int parentId = this.parentId;
          Analyze.this.parentSet(id, parentId);
          this.parentId = id;
          super.visit(id);
          this.parentId = parentId;
        }

        @Override
        protected void source(Node<org.zenframework.z8.blls.ast.Source> node) {
          super.source(node);
        }
      }.source(ast().node(parser.nodeId));

      return parser.nodeId;
    }
  }
}
