package org.zenframework.z8.blls.ast;

import java.util.stream.Stream;

public record Enumeration(
  int annotationsId,
  int modifiers,
  int nameId,
  int childrenId
) implements Node.Data {
  public Stream<Node<Annotation>> annotations(Ast ast) {
    return ast.siblings(annotationsId()).mapToObj(ast::node);
  }

  public String name(Ast ast) {
    return ast.string(nameId());
  }

  public Stream<Node<Child>> children(Ast ast) {
    return ast.siblings(childrenId()).mapToObj(ast::node);
  }

  public record Child(int nameId) implements Node.Data {
    public String name(Ast ast) {
      return ast.string(nameId());
    }
  }
}
