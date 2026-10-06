package org.zenframework.z8.blls.ast;

public record Import(int valueId) implements Node.Data {
  public Node<Definition.Reference> value(Ast ast) {
    return ast.node(valueId());
  }
}
