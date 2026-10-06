package org.zenframework.z8.blls.ast;

import java.util.Optional;
import java.util.stream.Stream;

public sealed interface Statement extends Node.Data {
  record Block(
    int statementsId
  ) implements Statement {
    public Stream<Node<? extends Statement>> statements(Ast ast) {
      return ast.siblings(statementsId()).mapToObj(ast::node);
    }
  }

  record Expression(
    int valueId
  ) implements Statement {
    public Node<org.zenframework.z8.blls.ast.Expression> value(Ast ast) {
      return ast.node(valueId());
    }
  }

  record Variable(
    int definitionId,
    int nameId,
    int valueId
  ) implements Statement {
    public Node<? extends Definition> definition(Ast ast) {
      return ast.node(definitionId());
    }

    public String name(Ast ast) {
      return ast.string(nameId());
    }

    public Optional<Node<org.zenframework.z8.blls.ast.Expression>> value(Ast ast) {
      return valueId() == 0 ? Optional.empty() : Optional.of(ast.node(valueId()));
    }
  }

  record Return(
    int valueId
  ) implements Statement {
    public Optional<Node<org.zenframework.z8.blls.ast.Expression>> value(Ast ast) {
      return valueId() == 0 ? Optional.empty() : Optional.of(ast.node(valueId()));
    }
  }

  record Break() implements Statement {
  }

  record Continue() implements Statement {
  }

  record Throw(
    int valueId
  ) implements Statement {
    public Node<org.zenframework.z8.blls.ast.Expression> value(Ast ast) {
      return ast.node(valueId());
    }
  }

  record Try(
    int blockId,
    int catchesId,
    int blockFinallyId
  ) implements Statement {
    public Node<Statement.Block> block(Ast ast) {
      return ast.node(blockId());
    }

    public Stream<Node<Statement.Try.Catch>> catches(Ast ast) {
      return ast.siblings(catchesId()).mapToObj(ast::node);
    }

    public Optional<Node<Statement.Block>> blockFinally(Ast ast) {
      return blockFinallyId() == 0 ? Optional.empty() : Optional.of(ast.node(blockFinallyId()));
    }

    public record Catch(
      int definitionId,
      int nameId,
      int blockId
    ) implements Node.Data {
      public Node<? extends Definition> definition(Ast ast) {
        return ast.node(definitionId());
      }

      public String name(Ast ast) {
        return ast.string(nameId());
      }

      public Node<Statement.Block> block(Ast ast) {
        return ast.node(blockId());
      }
    }
  }

  record Condition(
    int conditionId,
    int thenId,
    int otherId
  ) implements Statement {
    public Node<org.zenframework.z8.blls.ast.Expression> condition(Ast ast) {
      return ast.node(conditionId());
    }

    public Node<? extends Statement> then(Ast ast) {
      return ast.node(thenId());
    }

    public Optional<Node<? extends Statement>> other(Ast ast) {
      return otherId() == 0 ? Optional.empty() : Optional.of(ast.node(otherId()));
    }
  }

  record LoopDo(
    int statementId,
    int conditionId
  ) implements Statement {
    public Node<? extends Statement> statement(Ast ast) {
      return ast.node(statementId());
    }

    public Node<org.zenframework.z8.blls.ast.Expression.Paren> condition(Ast ast) {
      return ast.node(conditionId());
    }
  }

  record LoopWhile(
    int conditionId,
    int statementId
  ) implements Statement {
    public Node<org.zenframework.z8.blls.ast.Expression.Paren> condition(Ast ast) {
      return ast.node(conditionId());
    }

    public Node<? extends Statement> statement(Ast ast) {
      return ast.node(statementId());
    }
  }

  record LoopFor(
    int initializerId,
    int conditionId,
    int updateId,
    int statementId
  ) implements Statement {
    public Optional<Node<?>> initializer(Ast ast) {
      return initializerId() == 0 ? Optional.empty() : Optional.of(ast.node(initializerId()));
    }

    public Node<org.zenframework.z8.blls.ast.Expression> condition(Ast ast) {
      return ast.node(conditionId());
    }

    public Node<org.zenframework.z8.blls.ast.Expression> update(Ast ast) {
      return ast.node(updateId());
    }

    public Node<? extends Statement> statement(Ast ast) {
      return ast.node(statementId());
    }
  }

  record LoopForEach(
    int definitionId,
    int nameId,
    int expressionId,
    int statementId
  ) implements Statement {
    public Node<? extends Definition> definition(Ast ast) {
      return ast.node(definitionId());
    }

    public String name(Ast ast) {
      return ast.string(nameId());
    }

    public Node<org.zenframework.z8.blls.ast.Expression> expression(Ast ast) {
      return ast.node(expressionId());
    }

    public Node<? extends Statement> statement(Ast ast) {
      return ast.node(statementId());
    }
  }
}
