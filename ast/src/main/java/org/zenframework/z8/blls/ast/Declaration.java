package org.zenframework.z8.blls.ast;

import java.util.Optional;
import java.util.stream.Stream;

public record Declaration(int childrenId) implements Node.Data {
  public static final int PUBLIC = 1;
  public static final int PROTECTED = 2;
  public static final int PRIVATE = 4;
  public static final int STATIC = 8;
  public static final int FINAL = 16;
  public static final int VIRTUAL = 32;
  public static final int AUTO = 64;

  public Stream<Node<? extends Child>> children(Ast ast) {
    return ast.siblings(childrenId()).mapToObj(ast::node);
  }

  public sealed interface Child extends Node.Data {
  }

  public record Field(
    int annotationsId,
    int modifiers,
    int definitionId,
    int nameId,
    int valueId
  ) implements Child {
    public Stream<Node<Annotation>> annotations(Ast ast) {
      return ast.siblings(annotationsId()).mapToObj(ast::node);
    }

    public Node<? extends Definition> definition(Ast ast) {
      return ast.node(definitionId());
    }

    public String name(Ast ast) {
      return ast.string(nameId());
    }

    public Optional<Node<Statement.Expression>> value(Ast ast) {
      return valueId() == 0 ? Optional.empty() : Optional.of(ast.node(valueId()));
    }
  }

  public record Initialize(
    int annotationsId,
    int childId
  ) implements Child {
    public Stream<Node<Annotation>> annotations(Ast ast) {
      return ast.siblings(annotationsId()).mapToObj(ast::node);
    }

    public Node<Statement.Expression> child(Ast ast) {
      return ast.node(childId());
    }
  }

  public record Method(
    int annotationsId,
    int modifiers,
    int definitionId,
    int nameId,
    int parametersId,
    int blockId
  ) implements Child {
    public Stream<Node<Annotation>> annotations(Ast ast) {
      return ast.siblings(annotationsId()).mapToObj(ast::node);
    }

    public Node<? extends Definition> definition(Ast ast) {
      return ast.node(definitionId());
    }

    public String name(Ast ast) {
      return ast.string(nameId());
    }

    public Stream<Node<Parameter>> parameters(Ast ast) {
      return ast.siblings(parametersId()).mapToObj(ast::node);
    }

    public Optional<Node<Statement.Block>> block(Ast ast) {
      return blockId() == 0 ? Optional.empty() : Optional.of(ast.node(blockId()));
    }
  }

  public record Priority(
    int annotationsId,
    int modifiers,
    int parametersId,
    int blockId
  ) implements Child {
    public Stream<Node<Annotation>> annotations(Ast ast) {
      return ast.siblings(annotationsId()).mapToObj(ast::node);
    }

    public Stream<Node<Parameter>> parameters(Ast ast) {
      return ast.siblings(parametersId()).mapToObj(ast::node);
    }

    public Optional<Node<Statement.Block>> block(Ast ast) {
      return blockId() == 0 ? Optional.empty() : Optional.of(ast.node(blockId()));
    }
  }

  public record Cast(
    int annotationsId,
    int modifiers,
    int definitionId,
    int parametersId,
    int blockId
  ) implements Child {
    public Stream<Node<Annotation>> annotations(Ast ast) {
      return ast.siblings(annotationsId()).mapToObj(ast::node);
    }

    public Node<Definition.Reference> definition(Ast ast) {
      return ast.node(definitionId());
    }

    public Stream<Node<Parameter>> parameters(Ast ast) {
      return ast.siblings(parametersId()).mapToObj(ast::node);
    }

    public Optional<Node<Statement.Block>> block(Ast ast) {
      return blockId() == 0 ? Optional.empty() : Optional.of(ast.node(blockId()));
    }
  }

  public record Operator(
    int annotationsId,
    int modifiers,
    int definitionId,
    Operation operation,
    int parametersId,
    int blockId
  ) implements Child {
    public Stream<Node<Annotation>> annotations(Ast ast) {
      return ast.siblings(annotationsId()).mapToObj(ast::node);
    }

    public Node<? extends Definition> definition(Ast ast) {
      return ast.node(definitionId());
    }

    public Stream<Node<Parameter>> parameters(Ast ast) {
      return ast.siblings(parametersId()).mapToObj(ast::node);
    }

    public Optional<Node<Statement.Block>> block(Ast ast) {
      return blockId() == 0 ? Optional.empty() : Optional.of(ast.node(blockId()));
    }
  }

  public record Records(
    int childrenId
  ) implements Child {
    public Stream<Node<Child>> children(Ast ast) {
      return ast.siblings(childrenId()).mapToObj(ast::node);
    }

    public record Child(
      int annotationsId,
      int modifiers,
      int nameId,
      int valueId
    ) implements Node.Data {
      public Stream<Node<Annotation>> annotations(Ast ast) {
        return ast.siblings(annotationsId()).mapToObj(ast::node);
      }

      public String name(Ast ast) {
        return ast.string(nameId());
      }

      public Node<Expression.Special> value(Ast ast) {
        return ast.node(valueId());
      }
    }
  }
}
