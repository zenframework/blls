package org.zenframework.z8.blls.ast;

public record Parameter(int definitionId, int nameId) implements Node.Data {
  public Node<? extends Definition> definition(Ast ast) {
    return ast.node(definitionId());
  }

  public String name(Ast ast) {
    return ast.string(nameId());
  }
}
