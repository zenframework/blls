package org.zenframework.z8.blls.ast;

import java.util.Optional;
import java.util.stream.Stream;

public record Klass(
  int annotationsId,
  int modifiers,
  int nameId,
  int extendedId,
  int childId
) implements Node.Data {
  public Stream<Node<Annotation>> annotations(Ast ast) {
    return ast.siblings(annotationsId()).mapToObj(ast::node);
  }

  public String name(Ast ast) {
    return ast.string(nameId());
  }

  public Optional<Node<Definition.Reference>> extended(Ast ast) {
    return extendedId() == 0 ? Optional.empty() : Optional.of(ast.node(extendedId()));
  }

  public Node<Declaration> child(Ast ast) {
    return ast.node(childId());
  }
}
