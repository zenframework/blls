package org.zenframework.z8.blls.evalsql;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.analyzer.Caster;
import org.zenframework.z8.blls.ast.Declaration;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Node;
import org.zenframework.z8.blls.evalsql.expressions.Group;
import org.zenframework.z8.blls.evalsql.expressions.Operation;
import org.zenframework.z8.blls.evalsql.expressions.Unary;
import org.zenframework.z8.blls.evalsql.functions.conversion.IntToDecimal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public abstract class Visitor extends org.zenframework.z8.blls.ast.Visitor {
  protected @Nullable SqlToken token = null;
  private @Nullable SqlToken tokenOld = null;

  @Override
  protected abstract Ast ast();

  private void setToken(@Nullable SqlToken token) {
    this.tokenOld = token;
  }

  protected Optional<SqlToken> visitEval(Node<? extends Expression> node, int castId) {
    visit(node.id());

    var result = this.token;
    if (result == null) return Optional.empty();
    else return runCast(node, result, castId);
  }

  @Override
  public void visit(int id) {
    super.visit(id);

    this.token = tokenOld;
    this.tokenOld = null;
  }

  private Optional<SqlToken> runCast(Node<? extends Expression> node, SqlToken token, int castId) {
    if (castId == 0 || ast().knownCasts.containsKey(castId)) return Optional.of(token);
    else if (castId == ast().$sql_int$sql_decimal) return Optional.of(new IntToDecimal(node.id(), token));
    else return Optional.empty();
  }

  @Override
  protected void expressionParen(Node<Expression.Paren> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionParen.class);
    SqlToken result;
    if (e.resolvedPriorityId > 0) {
      Node<Declaration.Priority> priority = ast().node(e.resolvedPriorityId);
      var known = ast().knownPriorities.get(priority.id());
      if (known == null)
        result = visitEval(node.data().child(ast()), 0).orElse(null);
      else
        result = new Group(
          node.id(),
          visitEval(node.data().child(ast()), 0)
            .orElseGet(() -> new Uneval(node.id(), known.type()))
        );
    } else {
      result = visitEval(node.data().child(ast()), 0).or(() ->
        ast().type(node)
          .filter(it -> it.equals(new Expression.Type.Declaration(ast().knownOf(Ast.Known.$sql_binary).id())))
          .map(_ -> new Uneval(node.data().childId(), FieldType.Binary))
      ).orElse(null);
    }
    setToken(result);
  }

  @Override
  protected void expressionNull(Node<Expression.Null> node) {
    setToken(null);
  }

  @Override
  protected void expressionBoolean(Node<Expression.Boolean> node) {
    setToken(new SqlConst.Bool(node.id(), node.data().value()));
  }

  @Override
  protected void expressionInteger(Node<Expression.Integer> node) {
    setToken(new SqlConst.Integer(node.id(), node.data().value()));
  }

  @Override
  protected void expressionDecimal(Node<Expression.Decimal> node) {
    setToken(new SqlConst.Decimal(node.id(), node.data().value()));
  }

  @Override
  protected void expressionString(Node<Expression.String> node) {
    setToken(new SqlConst.String(node.id(), node.data().value(ast())));
  }

  @Override
  protected void expressionSpecial(Node<Expression.Special> node) {
    var type = ast().type(node).orElse(null);
    SqlToken result;
    if (type instanceof Expression.Type.Declaration(int id)) {
      if (id == ast().knownOf(Ast.Known.$date).id()) {
        result = new SqlConst.Date(
          node.id(),
          LocalDateTime
            .from(Ast.DATE_FORMATTER.parse(node.data().value(ast())))
            .toInstant(ZoneOffset.ofHours(-3))
            .toEpochMilli()
        );
      } else if (id == ast().knownOf(Ast.Known.$datespan).id()) {
        var m = Ast.REGEX_DATESPAN.matcher(node.data().value(ast()));
        if (m.matches()) {
          long millis = Integer.parseInt(m.group(1)) * Duration.ofDays(1).toMillis();
          millis += Integer.parseInt(m.group(2)) * Duration.ofHours(1).toMillis();
          millis += Integer.parseInt(m.group(3)) * Duration.ofMinutes(1).toMillis();
          millis += Integer.parseInt(m.group(4)) * Duration.ofSeconds(1).toMillis();
          result = new SqlConst.Datespan(node.id(), millis);
        } else {
          result = null;
        }
      } else if (id == ast().knownOf(Ast.Known.$guid).id()) {
        result = new SqlConst.Guid(node.id(), UUID.fromString(node.data().value(ast())));
      } else {
        result = null;
      }
    } else {
      result = null;
    }
    setToken(result);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void expressionArray(Node<Expression.Array> node) {
    if (node.data().childrenId() == 0) {
      setToken(null);
      return;
    }

    List<? extends @Nullable SqlToken> constants = node.data().children(ast()).map(it -> {
      if (it.data() instanceof Expression.Array.Child.Value v) {
        var e = ast().analyzer().extra(new Node<>(it.id(), v), Extra.ExpressionArrayChildValue.class);
        return visitEval(v.value(ast()), e.valueCastId).orElse(null);
      } else {
        visit(it.id());
        return null;
      }
    }).toList();

    var first = constants.getFirst();
    SqlToken result;
    if (first != null && constants.stream().allMatch(it ->
      it != null && it.type() == first.type() && it instanceof SqlConst
    )) {
      result = new ConstantsArray(node.id(), first.type(), (List<SqlConst>) constants);
    } else {
      result = null;
    }
    setToken(result);
  }

  @Override
  protected void expressionAccess(Node<Expression.Access> node) {
    super.expressionAccess(node);

    var e = ast().analyzer().extra(node, Extra.ExpressionAccess.class);
    SqlToken result = e.getResolved(ast())
      .map(it ->
        it instanceof Extra.ExpressionAccess.Resolved.DeclarationField(int id)
          ? ast().<Declaration.Field>node(id)
          : null
      )
      .map(it -> {
        var type = ast().analyzer().typeOf(it.data().definition(ast())).orElse(null);
        var known = type instanceof Expression.Type.Declaration(int id)
          ? ast().knownFields.get(id)
          : null;

        if (known != null) {
          return new SqlField(node.id(), known.type(), ast(), it);
        } else if (
          it.data().valueId() > 0 &&
            type != null &&
            Caster.runSimple(
              ast().analyzer(),
              type,
              new Expression.Type.Declaration(
                ast().knownOf(Ast.Known.$Expression).id()
              )
            )
        ) {
          var e2 = ast().analyzer().extra(it, Extra.DeclarationField.class);
          return visitEval(it.data().value(ast()).orElseThrow().data().value(ast()), e2.valueCastId).orElse(null);
        } else {
          return null;
        }
      })
      .orElse(null);
    setToken(result);
  }

  @Override
  protected void expressionInvoke(Node<Expression.Invoke> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionInvoke.class);
    SqlToken result = switch (e.getResolved(ast()).orElse(null)) {
      case Extra.ExpressionInvoke.Resolved.DeclarationMethod(int id) when !e.argumentsInvalid -> {
        var known = ast().knownMethods.get(id);
        if (known == null) {
          super.expressionInvoke(node);
          yield null;
        } else {
          var argumentsExpected = known.arguments.iterator();
          var arguments = node.data()
            .arguments(ast())
            .map(it -> {
              var e2 = ast().analyzer().extra(it, Extra.ExpressionInvokeArgument.class);
              var v = it.data().value(ast());
              var ae = argumentsExpected.next();
              return visitEval(v, e2.valueCastId).orElseGet(() -> new Uneval(v.id(), ae.type()));
            })
            .iterator();

          yield known instanceof Ast.Known.Method.Static v
            ? v.run(node.id(), null, arguments)
            : known.run(
            node.id(),
            visitEval(node.data().child(ast()).orElseThrow(), 0)
              .orElseGet(() -> new Uneval(node.data().childId(), known.declaration.type())),
            arguments
          );
        }
      }
      case Extra.ExpressionInvoke.Resolved.DeclarationCast(int id) -> {
        var c = node.data().child(ast()).orElseThrow();
        yield visitEval(c, 0)
          .flatMap(it -> runCast(c, it, id))
          .orElse(null);
      }
      case null, default -> {
        super.expressionInvoke(node);
        yield null;
      }
    };
    setToken(result);
  }

  @Override
  protected void expressionCast(Node<Expression.Cast> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionCast.class);
    SqlToken result;
    if (e.valueCastId == 0)
      result = visitEval(node.data().value(ast()), 0).orElse(null);
    else
      result = visitEval(node.data().value(ast()), 0)
        .flatMap(it -> runCast(node.data().value(ast()), it, e.valueCastId))
        .orElse(null);
    setToken(result);
  }

  @Override
  protected void expressionUnary(Node<Expression.Unary> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionUnary.class);
    var known = ast().knownOperatorsUnary.get(e.resolvedId);
    SqlToken result;
    if (known == null) {
      super.expressionUnary(node);
      result = null;
    } else {
      var tokenValue = visitEval(node.data().value(ast()), e.valueCastId)
        .orElseGet(() -> new Uneval(node.data().valueId(), known.declaration().type()));
      result = switch (known.operation()) {
        case org.zenframework.z8.blls.ast.Operation.ADD -> tokenValue;
        case org.zenframework.z8.blls.ast.Operation.NOT -> new Unary(node.id(), Operation.Not, tokenValue);
        case org.zenframework.z8.blls.ast.Operation.SUBTRACT -> new Unary(node.id(), Operation.Minus, tokenValue);
        default -> throw new AssertionError();
      };
    }
    setToken(result);
  }

  @Override
  protected void expressionBinary(Node<Expression.Binary> node) {
    var e = ast().analyzer().extra(node, Extra.ExpressionBinary.class);
    var known = ast().knownOperatorsBinary.get(e.resolvedId);
    SqlToken result;
    if (known == null) {
      super.expressionBinary(node);
      result = null;
    } else {
      var tokenLeft = visitEval(node.data().left(ast()), e.leftCastId)
        .orElseGet(() -> new Uneval(node.data().leftId(), known.declaration.type()));
      var tokenRight = visitEval(node.data().right(ast()), e.rightCastId)
        .orElseGet(() -> new Uneval(node.data().rightId(), known.right.type()));
      result = known.run(node.id(), tokenLeft, tokenRight);
    }
    setToken(result);
  }

  private static final class ConstantsArray extends SqlConst {
    private final FieldType type;
    private final List<SqlConst> constants;

    private ConstantsArray(int nodeId, FieldType type, List<SqlConst> constants) {
      super(nodeId);
      this.type = type;
      this.constants = constants;
    }

    @Override
    public java.lang.String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
      throw new UnsupportedOperationException();
    }

    @Override
    public FieldType type() {
      return type;
    }
  }
}
