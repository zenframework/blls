package org.zenframework.z8.blls.ast;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public sealed interface Expression extends Node.Data {
  sealed interface Type {
    record Null() implements Type {
    }

    record Declaration(int id) implements Type {
      public Declaration {
        assert id > 0;
      }

      public Node<org.zenframework.z8.blls.ast.Declaration> node(Ast ast) {
        return ast.node(id());
      }
    }

    record Enumeration(int id) implements Type {
      public Enumeration {
        assert id > 0;
      }

      public Node<org.zenframework.z8.blls.ast.Enumeration> node(Ast ast) {
        return ast.node(id());
      }
    }

    record Array(Type value) implements Type {
    }

    record Map(Type key, Type value) implements Type {
    }
  }

  record Paren(int childId) implements Expression {
    public Node<? extends Expression> child(Ast ast) {
      return ast.node(childId());
    }
  }

  record Null() implements Expression {
  }

  record Boolean(boolean value) implements Expression {
  }

  record Integer(long value) implements Expression {
  }

  record Decimal(double value) implements Expression {
  }

  record String(int valueId) implements Expression {
    public java.lang.String value(Ast ast) {
      return ast.string(valueId());
    }
  }

  record Special(int valueId) implements Expression {
    public static final Pattern REGEX_DATE = Pattern.compile("\\d\\d/\\d\\d/\\d\\d\\d\\d( \\d\\d:\\d\\d:\\d\\d)?");
    public static final Pattern REGEX_DATESPAN = Pattern.compile("\\d\\d \\d\\d:\\d\\d:\\d\\d");
    public static final Pattern REGEX_BINARY = Pattern.compile("\\d{1,3}(, \\d{1,3})*");
    public static final Pattern REGEX_GUID = Pattern.compile("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}");

    public java.lang.String value(Ast ast) {
      return ast.string(valueId());
    }
  }

  record This() implements Expression {
  }

  record Super() implements Expression {
  }

  record Container(int childId) implements Expression {
    public Optional<Node<Container>> child(Ast ast) {
      return childId == 0 ? Optional.empty() : Optional.of(ast.node(childId()));
    }
  }

  record New(int definitionId) implements Expression {
    public Node<? extends Definition> definition(Ast ast) {
      return ast.node(definitionId());
    }
  }

  record Array(
    int definitionId,
    int childrenId
  ) implements Expression {
    public Optional<Node<? extends Definition>> definition(Ast ast) {
      return definitionId() == 0 ? Optional.empty() : Optional.of(ast.node(definitionId()));
    }

    public Stream<Node<? extends Expression.Array.Child>> children(Ast ast) {
      return ast.siblings(childrenId()).mapToObj(ast::node);
    }

    public sealed interface Child extends Node.Data {
      record Value(int valueId) implements Child {
        public Node<? extends Expression> value(Ast ast) {
          return ast.node(valueId());
        }
      }

      record Entry(
        int keyId,
        int valueId
      ) implements Child {
        public Node<? extends Expression> key(Ast ast) {
          return ast.node(keyId());
        }

        public Node<? extends Expression> value(Ast ast) {
          return ast.node(valueId());
        }
      }
    }
  }

  record Access(
    int childId,
    int nameId
  ) implements Expression {
    public Optional<Node<? extends Expression>> child(Ast ast) {
      return childId() == 0 ? Optional.empty() : Optional.of(ast.node(childId()));
    }

    public java.lang.String name(Ast ast) {
      return ast.string(nameId());
    }
  }

  record Index(
    int childId,
    int valueId
  ) implements Expression {
    public Node<? extends Expression> child(Ast ast) {
      return ast.node(childId());
    }

    public Node<? extends Expression> value(Ast ast) {
      return ast.node(valueId());
    }
  }

  record Invoke(
    int childId,
    int nameId,
    int argumentsId
  ) implements Expression {
    public Optional<Node<? extends Expression>> child(Ast ast) {
      return childId() == 0 ? Optional.empty() : Optional.of(ast.node(childId()));
    }

    public java.lang.String name(Ast ast) {
      return ast.string(nameId());
    }

    public Stream<Node<Expression.Invoke.Argument>> arguments(Ast ast) {
      return ast.siblings(argumentsId()).mapToObj(ast::node);
    }

    public record Argument(int valueId) implements Node.Data {
      public Node<? extends Expression> value(Ast ast) {
        return ast.node(valueId());
      }
    }
  }

  record Cast(
    int definitionId,
    int valueId
  ) implements Expression {
    public Node<? extends Definition> definition(Ast ast) {
      return ast.node(definitionId());
    }

    public Node<? extends Expression> value(Ast ast) {
      return ast.node(valueId());
    }
  }

  record Object(
    int childId
  ) implements Expression {
    public Node<Declaration> child(Ast ast) {
      return ast.node(childId());
    }
  }

  record Unary(
    Operation operation,
    int valueId
  ) implements Expression {
    public Node<? extends Expression> value(Ast ast) {
      return ast.node(valueId());
    }
  }

  record Binary(
    int leftId,
    Operation operation,
    int rightId
  ) implements Expression {
    public Node<? extends Expression> left(Ast ast) {
      return ast.node(leftId());
    }

    public Node<? extends Expression> right(Ast ast) {
      return ast.node(rightId());
    }
  }

  record Instanceof(
    int valueId,
    int definitionId
  ) implements Expression {
    public Node<? extends Expression> value(Ast ast) {
      return ast.node(valueId());
    }

    public Node<? extends Definition> definition(Ast ast) {
      return ast.node(definitionId());
    }
  }

  record Ternary(
    int conditionId,
    int thenId,
    int otherId
  ) implements Expression {
    public Node<? extends Expression> condition(Ast ast) {
      return ast.node(conditionId());
    }

    public Node<? extends Expression> then(Ast ast) {
      return ast.node(thenId());
    }

    public Node<? extends Expression> other(Ast ast) {
      return ast.node(otherId());
    }
  }
}
