package org.zenframework.z8.blls.ast;

import java.util.Optional;

public sealed interface Definition extends Node.Data {
  record Reference(int childId, int nameId) implements Definition {
    public Optional<Node<Definition.Reference>> child(Ast ast) {
      return childId() == 0 ? Optional.empty() : Optional.of(ast.node(childId()));
    }

    public String name(Ast ast) {
      return ast.string(nameId());
    }
  }

  record Array(int valueId) implements Definition {
    public Node<? extends Definition> value(Ast ast) {
      return ast.node(valueId());
    }
  }

  record Map(int valueId, int keyId) implements Definition {
    public Node<? extends Definition> value(Ast ast) {
      return ast.node(valueId());
    }

    public Node<? extends Definition> key(Ast ast) {
      return ast.node(keyId());
    }
  }
}
