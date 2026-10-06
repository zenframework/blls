package org.zenframework.z8.blls.ast;

import java.util.stream.Stream;

public record Source(
  int importsId,
  int childrenId
) implements Node.Data {
  public Stream<Node<Import>> imports(Ast ast) {
    return ast.siblings(importsId()).mapToObj(ast::node);
  }

  public Stream<Node<?>> children(Ast ast) {
    return ast.siblings(childrenId()).mapToObj(ast::node);
  }
}
