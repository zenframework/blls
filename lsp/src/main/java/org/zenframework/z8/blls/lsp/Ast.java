package org.zenframework.z8.blls.lsp;

import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.MapInt;
import org.zenframework.z8.blls.analyzer.ResolverUnqualified;
import org.zenframework.z8.blls.analyzer.Synthetic;
import org.zenframework.z8.blls.ast.*;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

abstract class Ast extends org.zenframework.z8.blls.ast.Ast.Delegating {
  static final Comparator<Position> POSITION_COMPARATOR =
    Comparator.comparingInt(Position::getLine).thenComparingInt(Position::getCharacter);

  Ast(org.zenframework.z8.blls.ast.Ast ast) {
    super(ast);
  }

  static boolean hits(Range range, Position position) {
    return POSITION_COMPARATOR.compare(range.getStart(), position) <= 0 &&
      POSITION_COMPARATOR.compare(position, range.getEnd()) <= 0;
  }

  static boolean intersects(Range a, Range b) {
    return POSITION_COMPARATOR.compare(a.getStart(), b.getEnd()) <= 0 &&
      POSITION_COMPARATOR.compare(b.getStart(), a.getEnd()) <= 0;
  }

  static String operationZ8String(Operation operation) {
    return switch (operation) {
      case NOT, BIT_NOT -> "Not";
      case MULTIPLY -> "Mul";
      case DIVIDE -> "Div";
      case MODULE -> "Mod";
      case ADD -> "Add";
      case SUBTRACT -> "Sub";
      case LESS -> "Less";
      case LESS_EQUAL -> "LessEqu";
      case MORE -> "More";
      case MORE_EQUAL -> "MoreEqu";
      case EQUAL -> "Equ";
      case NOT_EQUAL -> "NotEqu";
      case AND -> "And";
      case OR -> "Or";
      case BIT_AND -> "BitwiseAnd";
      case BIT_OR -> "BitwiseOr";
      case BIT_XOR -> "Xor";
      case ASSIGN -> "Assign";
      default -> throw new AssertionError();
    };
  }

  protected abstract String uriOf(Node<?> node);

  org.zenframework.z8.blls.analyzer.Ast analyzer() {
    return new org.zenframework.z8.blls.analyzer.Ast(this);
  }

  Optional<org.zenframework.z8.blls.evalsql.Ast> evalsql() {
    try {
      return Optional.of(new org.zenframework.z8.blls.evalsql.Ast(this));
    } catch (org.zenframework.z8.blls.evalsql.Ast.Exception e) {
      return Optional.empty();
    }
  }

  void extraUpdate(Node<?> node, Consumer<Extra> action) {
    Extra e = extra(node.id(), Extra.class);
    action.accept(e);
    extraSet(node.id(), e);
  }

  Optional<String> docOf(Node<?> node) {
    {
      var e = extra(node.id(), Extra.class);
      if (e.doc != null) {
        //noinspection NullableProblems
        return Optional.of(e.doc);
      }
    }

    return switch (node.data()) {
      case Klass v -> v.extended(this)
        .flatMap(analyzer()::typeOf)
        .orElse(null)
        instanceof Expression.Type.Declaration v2
        ? docOf(v2.node(this))
        : Optional.empty();
      case org.zenframework.z8.blls.ast.Declaration.Method v -> {
        var e = extra(node.id(), org.zenframework.z8.blls.analyzer.Ast.Extra.DeclarationMethod.class);
        yield e.overrideId == 0 ? Optional.empty() : docOf(node(e.overrideId));
      }
      case org.zenframework.z8.blls.ast.Declaration.Priority v -> {
        var e = extra(node.id(), org.zenframework.z8.blls.analyzer.Ast.Extra.DeclarationPriority.class);
        yield e.overrideId == 0 ? Optional.empty() : docOf(node(e.overrideId));
      }
      case org.zenframework.z8.blls.ast.Declaration.Cast v -> {
        var e = extra(node.id(), org.zenframework.z8.blls.analyzer.Ast.Extra.DeclarationCast.class);
        yield e.overrideId == 0 ? Optional.empty() : docOf(node(e.overrideId));
      }
      case org.zenframework.z8.blls.ast.Declaration.Operator v -> {
        var e = extra(node.id(), org.zenframework.z8.blls.analyzer.Ast.Extra.DeclarationOperator.class);
        yield e.overrideId == 0 ? Optional.empty() : docOf(node(e.overrideId));
      }
      default -> Optional.empty();
    };
  }

  // ast accessors

  Range rangeOfModifiers(Node<?> node, Stream<Node<Annotation>> annotations) {
    var modifiers = annotations
      .reduce((_, b) -> b)
      .map(it -> tokensNext(end(it).id()).skip(1))
      .orElseGet(() -> tokensNext(start(node).id()))
      .filter(it -> switch (it.kind()) {
        case Token.Kind.KEYWORD_PUBLIC,
             Token.Kind.KEYWORD_PROTECTED,
             Token.Kind.KEYWORD_PRIVATE,
             Token.Kind.KEYWORD_STATIC,
             Token.Kind.KEYWORD_FINAL,
             Token.Kind.KEYWORD_VIRTUAL,
             Token.Kind.KEYWORD_AUTO -> true;
        default -> false;
      }).toList();
    return range(modifiers.getFirst(), modifiers.getLast());
  }

  Token importKeyword(Node<Import> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_IMPORT;
    return result;
  }

  Token klassName(Node<Klass> node) {
    var result = node.data().annotations(this)
      .reduce((_, b) -> b)
      .map(it -> tokensNext(end(it).id()).skip(1))
      .orElseGet(() -> tokensNext(start(node).id()))
      .filter(it -> it.kind() == Token.Kind.IDENTIFIER)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token enumerationName(Node<org.zenframework.z8.blls.ast.Enumeration> node) {
    var result = node.data().annotations(this)
      .reduce((_, b) -> b)
      .map(it -> tokensNext(end(it).id()).skip(1))
      .orElseGet(() -> tokensNext(start(node).id()))
      .filter(it -> it.kind() == Token.Kind.IDENTIFIER)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token annotationName(Node<Annotation> node) {
    var result = tokensNext(start(node).id())
      .skip(1)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token parameterName(Node<Parameter> node) {
    var result = end(node);
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token declarationFieldName(Node<org.zenframework.z8.blls.ast.Declaration.Field> node) {
    var result = node.data().annotations(this)
      .reduce((_, b) -> b)
      .map(it -> tokensNext(end(it).id()).skip(1))
      .orElseGet(() -> tokensNext(start(node).id()))
      .takeWhile(it -> it.kind() != Token.Kind.SEMICOLON && it.kind() != Token.Kind.EQUAL)
      .reduce((_, b) -> b)
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Optional<Token> declarationFieldOperation(Node<org.zenframework.z8.blls.ast.Declaration.Field> node) {
    return node.data().value(this).map(it -> {
      var result = tokensBack(start(it).id())
        .skip(1)
        .findFirst()
        .orElseThrow();
      assert result.kind() == Token.Kind.EQUAL;
      return result;
    });
  }

  Token declarationMethodName(Node<org.zenframework.z8.blls.ast.Declaration.Method> node) {
    var result = node.data().annotations(this)
      .reduce((_, b) -> b)
      .map(it -> tokensNext(end(it).id()).skip(1))
      .orElseGet(() -> tokensNext(start(node).id()))
      .takeWhile(it -> it.kind() != Token.Kind.PAREN_L)
      .reduce((_, b) -> b)
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  private Token declarationPriorityCastOperatorKeyword(Node<?> node) {
    var result = tokensNext(start(node).id())
      .filter(it -> it.kind() == Token.Kind.KEYWORD_OPERATOR)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.KEYWORD_OPERATOR;
    return result;
  }

  Token declarationPriorityKeyword(Node<org.zenframework.z8.blls.ast.Declaration.Priority> node) {
    return declarationPriorityCastOperatorKeyword(node);
  }

  Token declarationCastKeyword(Node<org.zenframework.z8.blls.ast.Declaration.Cast> node) {
    return declarationPriorityCastOperatorKeyword(node);
  }

  Token declarationOperatorKeyword(Node<org.zenframework.z8.blls.ast.Declaration.Operator> node) {
    return declarationPriorityCastOperatorKeyword(node);
  }

  Token declarationOperatorOperation(Node<org.zenframework.z8.blls.ast.Declaration.Operator> node) {
    return tokensNext(declarationOperatorKeyword(node).id())
      .skip(1)
      .findFirst()
      .orElseThrow();
  }

  Token declarationRecordsKeyword(Node<org.zenframework.z8.blls.ast.Declaration.Records> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_RECORDS;
    return result;
  }

  Token declarationRecordsChildName(Node<org.zenframework.z8.blls.ast.Declaration.Records.Child> node) {
    var result = node.data().annotations(this)
      .reduce((_, b) -> b)
      .map(it -> tokensNext(end(it).id()).skip(1))
      .orElseGet(() -> tokensNext(start(node).id()))
      .filter(it -> it.kind() == Token.Kind.IDENTIFIER)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token enumerationChildName(Node<org.zenframework.z8.blls.ast.Enumeration.Child> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token statementVariableName(Node<Statement.Variable> node) {
    var result = tokensNext(end(node.data().definition(this)).id())
      .skip(1)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Optional<Token> statementVariableOperation(Node<Statement.Variable> node) {
    return node.data().value(this).map(it -> {
      var result = tokensBack(start(it).id())
        .skip(1)
        .findFirst()
        .orElseThrow();
      assert result.kind() == Token.Kind.EQUAL;
      return result;
    });
  }

  Token statementReturnKeyword(Node<Statement.Return> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_RETURN;
    return result;
  }

  Token statementThrowKeyword(Node<Statement.Throw> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_THROW;
    return result;
  }

  Token statementTryKeyword(Node<Statement.Try> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_TRY;
    return result;
  }

  Token statementConditionKeyword(Node<Statement.Condition> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_IF;
    return result;
  }

  Token statementLoopDoKeywordDo(Node<Statement.LoopDo> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_DO;
    return result;
  }

  Token statementLoopDoKeywordWhile(Node<Statement.LoopDo> node) {
    var result = tokensNext(end(node.data().statement(this)).id())
      .skip(1)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.KEYWORD_DO;
    return result;
  }

  Token statementLoopWhileKeyword(Node<Statement.LoopWhile> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_WHILE;
    return result;
  }

  Token statementLoopForKeyword(Node<Statement.LoopFor> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_FOR;
    return result;
  }

  Token statementLoopForEachKeyword(Node<Statement.LoopForEach> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_FOR;
    return result;
  }

  Token statementLoopForEachName(Node<Statement.LoopForEach> node) {
    var result = tokensNext(end(node.data().definition(this)).id())
      .skip(1)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token statementTryCatchName(Node<Statement.Try.Catch> node) {
    var result = tokensNext(start(node).id())
      .takeWhile(it -> it.kind() != Token.Kind.PAREN_R)
      .reduce((_, b) -> b)
      .orElseThrow();
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token expressionContainerKeyword(Node<Expression.Container> node) {
    var result = end(node);
    assert result.kind() == Token.Kind.KEYWORD_CONTAINER;
    return result;
  }

  Token expressionNewKeyword(Node<Expression.New> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_NEW;
    return result;
  }

  Token expressionAccessName(Node<Expression.Access> node) {
    var result = end(node);
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token expressionInvokeName(Node<Expression.Invoke> node) {
    var result = node.data().child(this)
      .map(it -> tokensNext(end(it).id())
        .skip(2)
        .findFirst()
        .orElseThrow()
      )
      .orElseGet(() -> start(node));
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Token expressionObjectKeyword(Node<Expression.Object> node) {
    var result = start(node);
    assert result.kind() == Token.Kind.KEYWORD_CLASS;
    return result;
  }

  Token expressionUnaryOperation(Node<Expression.Unary> node) {
    var result = start(node);
    assert switch (result.kind()) {
      case Token.Kind.PLUS, Token.Kind.MINUS, Token.Kind.EXCLAMATION -> true;
      default -> false;
    };
    return result;
  }

  Token expressionBinaryOperation(Node<Expression.Binary> node) {
    return tokensNext(end(node.data().left(this)).id())
      .skip(1)
      .findFirst()
      .orElseThrow();
  }

  Token expressionInstanceofKeyword(Node<Expression.Instanceof> node) {
    var result = tokensNext(end(node.data().value(this)).id())
      .skip(1)
      .findFirst()
      .orElseThrow();
    assert result.kind() == Token.Kind.KEYWORD_INSTANCEOF;
    return result;
  }

  Token definitionReferenceName(Node<Definition.Reference> node) {
    var result = end(node);
    assert result.kind() == Token.Kind.IDENTIFIER;
    return result;
  }

  Range anchor(Node<?> node) {
    return switch (node.data()) {
      case Import v -> range(importKeyword(new Node<>(node.id(), v)));
      case Klass v -> range(klassName(new Node<>(node.id(), v)));
      case org.zenframework.z8.blls.ast.Enumeration v -> range(enumerationName(new Node<>(node.id(), v)));
      case Annotation v -> range(annotationName(new Node<>(node.id(), v)));
      case Parameter v -> range(parameterName(new Node<>(node.id(), v)));
      case org.zenframework.z8.blls.ast.Declaration _ -> anchor(parent(node.id()));
      case org.zenframework.z8.blls.ast.Declaration.Field v -> range(declarationFieldName(new Node<>(node.id(), v)));
      case org.zenframework.z8.blls.ast.Declaration.Initialize v -> switch (v.child(this).data().value(this)) {
        case Node(int id, Expression.Access v2) -> range(new Node<>(id, v2));
        case Node(int _, Expression.Binary v2) -> range(v2.left(this));
        default -> throw new AssertionError();
      };
      case org.zenframework.z8.blls.ast.Declaration.Method v -> range(declarationMethodName(new Node<>(node.id(), v)));
      case org.zenframework.z8.blls.ast.Declaration.Priority _,
           org.zenframework.z8.blls.ast.Declaration.Cast _,
           org.zenframework.z8.blls.ast.Declaration.Operator _ -> range(declarationPriorityCastOperatorKeyword(node));
      case org.zenframework.z8.blls.ast.Declaration.Records v ->
        range(declarationRecordsKeyword(new Node<>(node.id(), v)));
      case org.zenframework.z8.blls.ast.Declaration.Records.Child v ->
        range(declarationRecordsChildName(new Node<>(node.id(), v)));
      case org.zenframework.z8.blls.ast.Enumeration.Child v -> range(enumerationChildName(new Node<>(node.id(), v)));
      case Statement.Variable v -> range(statementVariableName(new Node<>(node.id(), v)));
      case Statement.Return v -> range(statementReturnKeyword(new Node<>(node.id(), v)));
      case Statement.Throw v -> range(statementThrowKeyword(new Node<>(node.id(), v)));
      case Statement.Try v -> range(statementTryKeyword(new Node<>(node.id(), v)));
      case Statement.Condition v -> range(statementConditionKeyword(new Node<>(node.id(), v)));
      case Statement.LoopDo v -> range(statementLoopDoKeywordDo(new Node<>(node.id(), v)));
      case Statement.LoopWhile v -> range(statementLoopWhileKeyword(new Node<>(node.id(), v)));
      case Statement.LoopFor v -> range(statementLoopForKeyword(new Node<>(node.id(), v)));
      case Statement.LoopForEach v -> range(statementLoopForEachKeyword(new Node<>(node.id(), v)));
      case Statement.Try.Catch _ -> {
        var result = start(node);
        assert result.kind() == Token.Kind.KEYWORD_CATCH;
        yield range(result);
      }
      case Expression.New v -> range(expressionNewKeyword(new Node<>(node.id(), v)));
      case Expression.Access v -> range(expressionAccessName(new Node<>(node.id(), v)));
      case Expression.Invoke v -> range(expressionInvokeName(new Node<>(node.id(), v)));
      case Expression.Object v -> range(expressionObjectKeyword(new Node<>(node.id(), v)));
      case Expression.Unary v -> range(expressionUnaryOperation(new Node<>(node.id(), v)));
      case Expression.Binary v -> range(expressionBinaryOperation(new Node<>(node.id(), v)));
      case Expression.Instanceof v -> range(expressionInstanceofKeyword(new Node<>(node.id(), v)));
      case Source _,
           Statement.Block _,
           Statement.Expression _,
           Statement.Break _,
           Statement.Continue _,
           Expression.Paren _,
           Expression.Null _,
           Expression.Boolean _,
           Expression.Integer _,
           Expression.Decimal _,
           Expression.String _,
           Expression.Special _,
           Expression.This _,
           Expression.Super _,
           Expression.Container _,
           Expression.Array _,
           Expression.Index _,
           Expression.Cast _,
           Expression.Ternary _,
           Expression.Array.Child.Value _,
           Expression.Array.Child.Entry _,
           Expression.Invoke.Argument _,
           Definition.Reference _,
           Definition.Array _,
           Definition.Map _ -> range(node);
    };
  }

  Range range(Node<?> node) {
    var end = end(node);
    return range(
      start(node),
      tokensNextWhitespace(end.id())
        .skip(1)
        .findFirst()
        .orElse(end)
    );
  }

  Range range(Token token) {
    return range(
      token,
      tokensNextWhitespace(token.id())
        .skip(1)
        .findFirst()
        .orElse(token)
    );
  }

  Range range(Token start, Token end) {
    return new Range(position(start), position(end));
  }

  Position position(Token token) {
    return new Position(token.line(), token.character());
  }

  // signature

  String nameOfDeclaration(Node<org.zenframework.z8.blls.ast.Declaration> node) {
    return this.<Klass>parent(node.id()).data().name(this);
  }

  String toString(org.zenframework.z8.blls.analyzer.Synthetic.Method synthetic) {
    return "%s(%s)".formatted(
      synthetic.name(),
      synthetic.parameters()
        .stream()
        .map(org.zenframework.z8.blls.analyzer.Synthetic.Parameter::name)
        .collect(joining(", "))
    );
  }

  String toString(Expression.Type type) {
    return switch (type) {
      case Expression.Type.Null _ -> "null";
      case Expression.Type.Declaration v -> analyzer()
        .inheritanceKlasses(v.node(this))
        .findFirst()
        .map(it -> it.data().name(this))
        .orElse("object");
      case Expression.Type.Enumeration v -> v.node(this).data().name(this);
      case Expression.Type.Array(var value) -> "%s[]".formatted(toString(value));
      case Expression.Type.Map(var key, var value) -> "%s[%s]".formatted(toString(value), toString(key));
    };
  }

  String toMarkdown(Expression.Type type) {
    return switch (type) {
      case Expression.Type.Null _ -> "null";
      case Expression.Type.Declaration(int id) -> switch (parent(id)) {
        case Node(int id2, Klass v) -> "[%s](%s)".formatted(v.name(this), uriOf(new Node<>(id2, v)));
        case Node(int id2, Expression.Object v) -> "[object](%s)".formatted(uriOf(new Node<>(id2, v)));
        default -> throw new AssertionError();
      };
      case Expression.Type.Enumeration v -> "`enum` [%s](%s)".formatted(
        v.node(this).data().name(this),
        uriOf(v.node(this))
      );
      case Expression.Type.Array(var value) -> "%s[]".formatted(toMarkdown(value));
      case Expression.Type.Map(var key, var value) -> "%s[%s]".formatted(toMarkdown(value), toMarkdown(key));
    };
  }

  MarkupContent markupOfDeclarationField(Node<org.zenframework.z8.blls.ast.Declaration.Field> node) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%s %s::%s%s".formatted(
        analyzer().typeOf(node.data().definition(this)).map(this::toMarkdown).orElse("<error>"),
        toMarkdown(new Expression.Type.Declaration(analyzer().declarationOf(node).id())),
        node.data().name(this),
        docOf(node).map(it -> "\n\n" + it).orElse("")
      )
    );
  }

  MarkupContent markupOfDeclarationMethod(Node<org.zenframework.z8.blls.ast.Declaration.Method> node) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%s %s::%s(%s)%s".formatted(
        analyzer().typeOf(node.data().definition(this)).map(this::toMarkdown).orElse("<error>"),
        toMarkdown(new Expression.Type.Declaration(analyzer().declarationOf(node).id())),
        node.data().name(this),
        node.data()
          .parameters(this)
          .map(it ->
            "%s %s".formatted(
              analyzer().typeOf(it.data().definition(this)).map(this::toMarkdown).orElse("<error>"),
              it.data().name(this)
            )
          )
          .collect(joining(", ")),
        docOf(node).map(it -> "\n\n" + it).orElse("")
      )
    );
  }

  MarkupContent markupOfSyntheticMethod(Synthetic.Method synthetic) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%s %s::%s(%s)".formatted(
        toMarkdown(synthetic.result()),
        toMarkdown(synthetic.source()),
        synthetic.name(),
        synthetic.parameters()
          .stream()
          .map(it -> "%s %s".formatted(toMarkdown(it.type()), it.name()))
          .collect(joining(", "))
      )
    );
  }

  MarkupContent markupOfDeclarationPriority(Node<org.zenframework.z8.blls.ast.Declaration.Priority> node) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%1$s %1$s::operatorPriority()%2$s".formatted(
        toString(new Expression.Type.Declaration(analyzer().declarationOf(node).id())),
        docOf(node).map(it -> "\n\n" + it).orElse("")
      )
    );
  }

  MarkupContent markupOfDeclarationCast(Node<org.zenframework.z8.blls.ast.Declaration.Cast> node) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%s %s::%s()%s".formatted(
        analyzer().typeOf(node.data().definition(this)).map(this::toMarkdown).orElse("<error>"),
        toMarkdown(new Expression.Type.Declaration(analyzer().declarationOf(node).id())),
        node.data().definition(this).data().name(this),
        docOf(node).map(it -> "\n\n" + it).orElse("")
      )
    );
  }

  MarkupContent markupOfDeclarationOperator(Node<org.zenframework.z8.blls.ast.Declaration.Operator> node) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%s %s::operator%s(%s)%s".formatted(
        analyzer().typeOf(node.data().definition(this)).map(this::toMarkdown).orElse("<error>"),
        toMarkdown(new Expression.Type.Declaration(analyzer().declarationOf(node).id())),
        operationZ8String(node.data().operation()),
        node.data()
          .parameters(this)
          .map(it ->
            "%s %s".formatted(
              analyzer().typeOf(it.data().definition(this)).map(this::toMarkdown).orElse("<error>"),
              it.data().name(this)
            )
          )
          .collect(joining(", ")),
        docOf(node).map(it -> "\n\n" + it).orElse("")
      )
    );
  }

  MarkupContent markupOfSyntheticOperator(Synthetic.Operator synthetic) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%s %s::operator%s(%s)".formatted(
        toMarkdown(synthetic.result()),
        toMarkdown(synthetic.source()),
        operationZ8String(synthetic.operation()),
        synthetic.parameters()
          .stream()
          .map(it -> "%s %s".formatted(toMarkdown(it.type()), it.name()))
          .collect(joining(", "))
      )
    );
  }

  MarkupContent markupOfDeclarationRecordsChild(Node<org.zenframework.z8.blls.ast.Declaration.Records.Child> node) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%s %s::`records`::%s%s".formatted(
        type(node.data().value(this)).map(this::toMarkdown).orElse("<error>"),
        toString(new Expression.Type.Declaration(analyzer().declarationOf(node).id())),
        node.data().name(this),
        docOf(node).map(it -> "\n\n" + it).orElse("")
      )
    );
  }

  MarkupContent markupOfEnumerationChild(Node<org.zenframework.z8.blls.ast.Enumeration.Child> node) {
    return new MarkupContent(
      MarkupKind.MARKDOWN,
      "%s::%s%s".formatted(
        toMarkdown(new Expression.Type.Enumeration(parent(node.id()).id())),
        node.data().name(this),
        docOf(node).map(it -> "\n\n" + it).orElse("")
      )
    );
  }

  static final class Extra {
    @Nullable String doc;
  }

  // analyzer visitors

  abstract class Visitor1 extends org.zenframework.z8.blls.analyzer.Visitor1 {
    protected final MapInt<?>.Keys typeDependencies = new MapInt<>().keySet();
    private final org.zenframework.z8.blls.analyzer.Ast ast = new org.zenframework.z8.blls.analyzer.Ast(new Delegating(Ast.this) {
      private final MapInt<Object> extraLocal = new MapInt<>();

      @SuppressWarnings("unchecked")
      @Override
      public <E> E extra(int nodeId, Class<E> klass) {
        E result = (E) extraLocal.get(nodeId);
        return result == null ? super.extra(nodeId, klass) : result;
      }

      @Override
      public void extraSet(int nodeId, Object value) {
        switch (value) {
          case org.zenframework.z8.blls.analyzer.Ast.Extra.DefinitionReference v when v.resolvedId > 0 ->
            typeDependencies.add(v.resolvedId);
          case org.zenframework.z8.blls.analyzer.Ast.Extra.ExpressionAccess v -> {
            extraLocal.put(nodeId, value);
            if (v.resolvedId > 0)
              typeDependencies.add(v.resolvedId);
            return;
          }
          default -> {
          }
        }
        super.extraSet(nodeId, value);
      }
    });

    protected abstract void errorOf(Range range, String message, Node<?>... related);

    @Override
    protected org.zenframework.z8.blls.analyzer.Ast ast() {
      return ast;
    }

    @Override
    protected void diagnosticDefinitionReferenceAbsent(Node<Definition.Reference> node, Pack pack) {
      errorOf(anchor(node), "Не содержит %s".formatted(node.data().name(ast())));
    }

    @Override
    protected void diagnosticDefinitionReferenceExcess(Node<Definition.Reference> node, Node<?> result) {
      errorOf(anchor(node), "%s: разрешение достигло объявления".formatted(switch (result.data()) {
        case org.zenframework.z8.blls.ast.Enumeration v -> v.name(ast());
        case org.zenframework.z8.blls.ast.Declaration v -> nameOfDeclaration(new Node<>(result.id(), v));
        default -> throw new AssertionError();
      }));
    }

    @Override
    protected void diagnosticDefinitionReferenceLack(Node<Definition.Reference> node, Pack pack) {
      errorOf(anchor(node), "Не удалось разрешить");
    }

    @Override
    protected void expressionAccess(Node<Expression.Access> node) {
      super.expressionAccess(node);

      if (node.data().childId() == 0) {
        new ResolverUnqualified() {
          @Override
          protected org.zenframework.z8.blls.ast.Ast ast() {
            return Ast.this;
          }

          @Override
          protected Optional<Expression.Type.Declaration> primaryOf(String name) {
            return Visitor1.this.primaryOf(name);
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
          }

          @Override
          protected void success(PackChild result) {
            if (result instanceof Pack) {
              Visitor1.this.ast().extraUpdate(
                node,
                org.zenframework.z8.blls.analyzer.Ast.Extra.ExpressionAccess.class,
                e -> e.setResolved(result)
              );
            } else typeDependencies.add(result.id());
          }

          @Override
          protected void successImport(Node<Import> node) {
          }
        }.run();
      } else if (
        node.data().child(ast())
          .stream()
          .gather(org.zenframework.z8.blls.analyzer.Ast.only(Expression.Access.class))
          .flatMap(it ->
            ast()
              .extra(it, org.zenframework.z8.blls.analyzer.Ast.Extra.ExpressionAccess.class)
              .getResolved(ast())
              .stream()
          )
          .findFirst()
          .orElse(null)
          instanceof org.zenframework.z8.blls.analyzer.Ast.Extra.ExpressionAccess.Resolved.Pack v
      ) {
        // static pack access
        switch (ast().packGet(v.pack(), node.data().name(ast())).orElse(null)) {
          case null -> {
          }
          case Pack v2 -> ast().extraUpdate(
            node,
            org.zenframework.z8.blls.analyzer.Ast.Extra.ExpressionAccess.class,
            e -> e.setResolved(v2)
          );
          case PackChild v2 -> typeDependencies.add(v2.id());
        }
      }
    }
  }

  abstract class Visitor2 extends org.zenframework.z8.blls.analyzer.Visitor2 {
    protected abstract void warningOf(Range range, String message, Node<?>... related);

    protected abstract void errorOf(Range range, String message, Node<?>... related);

    @Override
    protected org.zenframework.z8.blls.analyzer.Ast ast() {
      return Ast.this.analyzer();
    }

    @Override
    protected void diagnosticAbsent(Node<?> node, Expression.Type source, Operation operation) {
      errorOf(anchor(node), "%s: не содержит '%s'".formatted(Ast.this.toString(source), operation));
    }

    @Override
    protected void diagnosticAmbiguous(Node<?> node, Stream<? extends Node<? extends org.zenframework.z8.blls.ast.Declaration.Child>> candidates) {
      errorOf(anchor(node), "Не удалось выбрать подходящий метод", candidates.toArray(Node[]::new));
    }

    @Override
    protected void diagnosticWrong(Node<?> node, Expression.Type type) {
      errorOf(anchor(node), "Неправильный тип: %s".formatted(Ast.this.toString(type)));
    }

    @Override
    protected void diagnosticInstance(Node<?> node) {
      errorOf(anchor(node), "Не применимо в статическом контексте");
    }

    @Override
    protected void diagnosticType(Node<?> node, Expression.Type target, Expression.Type source) {
      errorOf(anchor(node), "Ожидается %s: дано %s".formatted(Ast.this.toString(target), Ast.this.toString(source)));
    }

    @Override
    protected void diagnosticTypeNoHint(Node<?> node) {
      errorOf(anchor(node), "Невозможно выявить тип");
    }

    @Override
    protected void diagnosticStatementReturnNothing(Node<?> node) {
      errorOf(anchor(node), "Метод не возвращает значени");
    }

    @Override
    protected void diagnosticStatementReturnSomething(Node<?> node, Expression.Type target) {
      errorOf(anchor(node), "Метод должен вернуть %s".formatted(Ast.this.toString(target)));
    }

    @Override
    protected void diagnosticExpressionSpecial(Node<Expression.Special> node) {
      errorOf(anchor(node), "Ожидается дата, временной отрезок, байты или GUID");
    }

    @Override
    protected void diagnosticExpressionSuper(Node<Expression.Super> node) {
      errorOf(anchor(node), "Нет наследования");
    }

    @Override
    protected void diagnosticExpressionContainer(Node<Expression.Container> node) {
      errorOf(anchor(node), "Нет вложения");
    }

    @Override
    protected void diagnosticExpressionArray(Node<Expression.Array> node) {
      errorOf(anchor(node), "Ожидается массив или словарь");
    }

    @Override
    protected void diagnosticExpressionArrayChildValue(Node<Expression.Array.Child.Value> node) {
      errorOf(anchor(node), "Ожидается пара ключ-значение");
    }

    @Override
    protected void diagnosticExpressionArrayChildEntry(Node<Expression.Array.Child.Entry> node) {
      errorOf(anchor(node), "Ожидается элемент массива");
    }

    @Override
    protected void diagnosticExpressionAccessAbsent(Node<Expression.Access> node, Expression.Type type) {
      errorOf(anchor(node), "%s: не содержит '%s'".formatted(Ast.this.toString(type), node.data().name(ast())));
    }

    @Override
    protected void diagnosticExpressionAccessFailed(Node<Expression.Access> node) {
      errorOf(anchor(node), "Не удалось разрешить");
    }

    @Override
    protected void diagnosticExpressionAccessStaticAbsent(Node<Expression.Access> node, Pack pack) {
      errorOf(anchor(node), "Пакет не содержит '%s'".formatted(node.data().name(ast())));
    }

    @Override
    protected void diagnosticExpressionAccessStaticAbsentDeclaration(Node<Expression.Access> node, Node<org.zenframework.z8.blls.ast.Declaration> declaration) {
      errorOf(anchor(node), "%s: не содержит '%s'".formatted(nameOfDeclaration(declaration), node.data().name(ast())));
    }

    @Override
    protected void diagnosticExpressionAccessStaticAbsentEnumeration(Node<Expression.Access> node, Node<org.zenframework.z8.blls.ast.Enumeration> enumeration) {
      errorOf(anchor(node), "%s: не содержит '%s'".formatted(enumeration.data().name(ast()), node.data().name(ast())));
    }

    @Override
    protected void diagnosticExpressionAccessStaticLack(Node<Expression.Access> node, PackChild result) {
      errorOf(anchor(node), "Не удалось разрешить");
    }

    @Override
    protected void diagnosticExpressionIndex(Node<Expression.Index> node) {
      errorOf(anchor(node), "Ожидается массив или словарь");
    }

    @Override
    protected void diagnosticExpressionInvokeAbsent(Node<Expression.Invoke> node, Expression.Type type) {
      errorOf(anchor(node), "%s: не содержит '%s'".formatted(Ast.this.toString(type), node.data().name(ast())));
    }

    @Override
    protected void diagnosticExpressionInvokeAmbiguousSynthetic(Node<Expression.Invoke> node, Stream<org.zenframework.z8.blls.analyzer.Synthetic.Method> candidates) {
      errorOf(anchor(node), "Не удалось выбрать подходящий метод" +
        candidates.map(it -> "\n- " + Ast.this.toString(it)).collect(joining("\n"))
      );
    }

    @Override
    protected void diagnosticExpressionInvokeStaticAbsentDeclaration(Node<Expression.Invoke> node, Node<org.zenframework.z8.blls.ast.Declaration> declaration) {
      errorOf(anchor(node), "%s: не содержит '%s'".formatted(nameOfDeclaration(declaration), node.data().name(ast())));
    }

    @Override
    protected void diagnosticExpressionInvokeStaticAbsentEnumeration(Node<Expression.Invoke> node, Node<org.zenframework.z8.blls.ast.Enumeration> enumeration) {
      errorOf(anchor(node), "%s: не содержит '%s'".formatted(enumeration.data().name(ast()), node.data().name(ast())));
    }

    @Override
    protected void diagnosticExpressionCastSuccess(Node<Expression.Cast> node, Expression.Type source, Expression.Type target) {
      warningOf(anchor(node), "Выражение всегда выполняется: %s равен или наследует %s".formatted(Ast.this.toString(source), Ast.this.toString(target)));
    }

    @Override
    protected void diagnosticExpressionCastFail(Node<Expression.Cast> node, Expression.Type source, Expression.Type target) {
      warningOf(anchor(node), "Если значение != null: выражение невыполнимо: %s нельзя привести к %s".formatted(Ast.this.toString(source), Ast.this.toString(target)));
    }

    @Override
    protected void diagnosticExpressionInstanceofSuccess(Node<Expression.Instanceof> node) {
      warningOf(anchor(node), "Если значение != null: выражение истинно");
    }

    @Override
    protected void diagnosticExpressionInstanceofFail(Node<Expression.Instanceof> node, Expression.Type source, Expression.Type target) {
      warningOf(anchor(node), "Выражение ложно: %s не наследует %s".formatted(Ast.this.toString(target), Ast.this.toString(source)));
    }
  }

  abstract class Visitor3 extends org.zenframework.z8.blls.analyzer.Visitor3 {
    protected abstract void warningOf(Range range, String message, Node<?>... related);

    protected abstract void errorOf(Range range, String message, Node<?>... related);

    @Override
    protected org.zenframework.z8.blls.analyzer.Ast ast() {
      return Ast.this.analyzer();
    }

    @Override
    protected void diagnosticDuplicate(Node<?> nodeOld, Node<?> nodeNew) {
      errorOf(anchor(nodeNew), "Повтор", nodeOld);
    }

    @Override
    protected void diagnosticType(Node<?> node, Expression.Type target, Expression.Type source) {
      errorOf(anchor(node), "Ожидается %s: дано %s".formatted(Ast.this.toString(target), Ast.this.toString(source)));
    }

    @Override
    protected void diagnosticModifiers(Node<?> node, int modifiers) {
      errorOf(
        switch (node.data()) {
          case Klass v -> rangeOfModifiers(node, v.annotations(ast()));
          case org.zenframework.z8.blls.ast.Enumeration v -> rangeOfModifiers(node, v.annotations(ast()));
          case org.zenframework.z8.blls.ast.Declaration.Field v -> rangeOfModifiers(node, v.annotations(ast()));
          case org.zenframework.z8.blls.ast.Declaration.Method v -> rangeOfModifiers(node, v.annotations(ast()));
          case org.zenframework.z8.blls.ast.Declaration.Priority v -> rangeOfModifiers(node, v.annotations(ast()));
          case org.zenframework.z8.blls.ast.Declaration.Cast v -> rangeOfModifiers(node, v.annotations(ast()));
          case org.zenframework.z8.blls.ast.Declaration.Operator v -> rangeOfModifiers(node, v.annotations(ast()));
          case org.zenframework.z8.blls.ast.Declaration.Records.Child v -> rangeOfModifiers(node, v.annotations(ast()));
          default -> throw new AssertionError();
        },
        "Недопустимые модификаторы"
      );
    }

    @Override
    protected void diagnosticAnnotationUnknown(Node<Annotation> node) {
      warningOf(anchor(node), "Неизвестный атрибут");
    }

    @Override
    protected void diagnosticAnnotationExpectNone(Node<Annotation> node) {
      errorOf(anchor(node), "Не ожидается значение");
    }

    @Override
    protected void diagnosticAnnotationExpectBoolean(Node<Annotation> node) {
      errorOf(anchor(node), "Ожидается true/false");
    }

    @Override
    protected void diagnosticAnnotationExpectString(Node<Annotation> node) {
      errorOf(anchor(node), "Ожидается строка");
    }

    @Override
    protected void diagnosticAnnotationExpectString(Node<Annotation> node, Pattern regex) {
      errorOf(anchor(node), "Ожидается значение: %s".formatted(regex));
    }

    @Override
    protected void diagnosticAnnotationExpectCron(Node<Annotation> node) {
      errorOf(anchor(node), """
        Ожидается выражение CRON
        <минуты> <часы> <день месяца> <месяц> <день недели>

        Примеры:
        *   *     * * *   - каждую минуту
        */5 *     * * *   - каждые 5 минут
        5   0     * * *   - каждый день в 00:05
        15  14    1 * *   - каждый первый день месяца в 14:15
        0   22    * * 1-5 - с понедельника по пятницу в 22:00
        23  */2   * * *   - 00:23, 02:23, ..., 22:23
        15  10,13 * * 1,4 - по понедельникам и четвергам в 10:15 и 13:15""");
    }

    @Override
    protected void diagnosticParameters(Node<? extends org.zenframework.z8.blls.ast.Declaration.Child> node) {
      errorOf(anchor(node), "Недопустимые параметры");
    }

    @Override
    protected void diagnosticDeclarationExtendsFinal(Node<org.zenframework.z8.blls.ast.Declaration> node, Node<Klass> extended) {
      errorOf(anchor(node), "%s: нельзя наследовать final класс".formatted(extended.data().name(ast())), extended);
    }

    @Override
    protected void diagnosticDeclarationExtendsEnumeration(Node<org.zenframework.z8.blls.ast.Declaration> node, Node<org.zenframework.z8.blls.ast.Enumeration> extended) {
      errorOf(anchor(node), "%s: нельзя наследовать перечисление".formatted(extended.data().name(ast())), extended);
    }

    @Override
    protected void diagnosticDeclarationExtendsTypeWrong(Node<org.zenframework.z8.blls.ast.Declaration> node, Expression.Type type) {
      errorOf(anchor(node), "%s: нельзя наследовать массив или словарь".formatted(Ast.this.toString(type)));
    }

    @Override
    protected void diagnosticDeclarationExtendsCycle(Stream<Node<Klass>> cycle) {
      var iter = cycle.iterator();
      var first = iter.next();
      var current = first;
      var sb = new StringBuilder("Цикл в наследовании");
      while (iter.hasNext()) {
        var i = iter.next();
        sb.append("\n%s наследует %s".formatted(current.data().name(ast()), i.data().name(ast())));
        current = i;
      }
      errorOf(anchor(first), sb.toString());
    }

    @Override
    protected void diagnosticDeclarationPrimaryConditions(Node<org.zenframework.z8.blls.ast.Declaration> node, Node<Klass> primary) {
      errorOf(
        anchor(node),
        """
          Наследование класса %s с атрибутом primary:
          - Или добавьте operator=
          - Или добавьте атрибут primary
          - Или добавьте атрибут native
          """.formatted(primary.data().name(ast())),
        primary
      );
    }

    @Override
    protected void diagnosticDeclarationChildOverride(Node<? extends org.zenframework.z8.blls.ast.Declaration.Child> node, Node<? extends org.zenframework.z8.blls.ast.Declaration.Child> extended) {
      errorOf(anchor(node), "Переопределение невозможно: родительское объявление не виртуальное", extended);
    }

    @Override
    protected void diagnosticDeclarationChildReturns(Node<? extends org.zenframework.z8.blls.ast.Declaration.Child> node) {
      errorOf(anchor(node), "Не все пути возвращают значение");
    }

    @Override
    protected void diagnosticDeclarationChildUnimplemented(Node<? extends org.zenframework.z8.blls.ast.Declaration.Child> node) {
      errorOf(anchor(node), "Требуется тело");
    }

    @Override
    protected void diagnosticDeclarationFieldModifierAuto(Node<org.zenframework.z8.blls.ast.Declaration.Field> node) {
      errorOf(anchor(node), "auto: ожидается массив значений неосновного типа");
    }

    @Override
    protected void diagnosticDeclarationFieldModifierStatic(Node<org.zenframework.z8.blls.ast.Declaration.Field> node) {
      errorOf(anchor(node), "static: применим только к основным типам или массивам основных типов");
    }

    @Override
    protected void diagnosticDeclarationFieldModifierFinal(Node<org.zenframework.z8.blls.ast.Declaration.Field> node) {
      errorOf(anchor(node), "final: требуется значение");
    }

    @Override
    protected void diagnosticDeclarationInitializeAnnotation(Node<org.zenframework.z8.blls.ast.Declaration.Initialize> node) {
      errorOf(anchor(node), "Атрибуты применимы только к полям неосновных типов");
    }

    @Override
    protected void diagnosticStatementVariable(Node<Statement.Variable> node, Node<Statement.Variable> shadowedBy) {
      errorOf(anchor(node), "Перекрывается", shadowedBy);
    }

    @Override
    protected void diagnosticStatementBreak(Node<Statement.Break> node) {
      errorOf(range(start(node)), "Вне цикла");
    }

    @Override
    protected void diagnosticStatementContinue(Node<Statement.Continue> node) {
      errorOf(range(start(node)), "Вне цикла");
    }

    @Override
    protected void diagnosticStatementUnreachable(Node<? extends Statement> node) {
      errorOf(range(node), "Недостижимый код");
    }

    @Override
    protected void diagnosticStatementTryCatchUnreachable(Node<Statement.Try.Catch> node, Node<Statement.Try.Catch> shadowedBy) {
      errorOf(anchor(node), "Недостижимый код", shadowedBy);
    }

    @Override
    protected void diagnosticExpressionObject(Node<Expression.Object> node) {
      errorOf(anchor(node), "Класс-выражение поддерживается только в полях и переменных");
    }
  }

  abstract class Visitor4 extends org.zenframework.z8.blls.analyzer.Visitor4 {
    protected abstract void warningOf(Range range, String message, Node<?>... related);

    protected abstract void informationOf(Range range, String message, Node<?>... related);

    @Override
    protected org.zenframework.z8.blls.analyzer.Ast ast() {
      return Ast.this.analyzer();
    }

    @Override
    protected void diagnosticImportUnused(Node<Import> node) {
      warningOf(anchor(node), "Не используется");
    }

    @Override
    protected void diagnosticDeprecated(Node<?> node, Node<?> target, Node<Annotation> annotationDeprecated) {
      warningOf(
        anchor(node),
        annotationDeprecated.data()
          .value(ast())
          .map(Node::data)
          .orElse(null)
          instanceof Expression.String v
          ? v.value(ast())
          : "Устарело"
      );
    }

    @Override
    protected void diagnosticStatic(Node<?> node, Node<?> target) {
      warningOf(anchor(node), "Доступ к статическому элементу из объектного контекста", target);
    }

    @Override
    protected void diagnosticImplicitCast(Node<?> node, Node<org.zenframework.z8.blls.ast.Declaration.Cast> cast) {
      informationOf(anchor(node), "Неявное использование оператора преобразования", cast);
    }

    @Override
    protected void diagnosticDuplicate(Node<org.zenframework.z8.blls.ast.Declaration.Initialize> nodeOld, Node<org.zenframework.z8.blls.ast.Declaration.Initialize> nodeNew) {
      warningOf(anchor(nodeNew), "Повторное объявление", nodeOld);
    }
  }
}
