package org.zenframework.z8.blls.ast;

public record Node<D extends Node.Data>(int id, D data) {
  @Override
  public boolean equals(Object other) {
    return other instanceof Node<?> o && id() == o.id();
  }

  @Override
  public int hashCode() {
    return id();
  }

  public sealed interface Data permits Annotation, Declaration, Declaration.Child, Declaration.Records.Child, Definition, Enumeration, Enumeration.Child, Expression, Expression.Array.Child, Expression.Invoke.Argument, Import, Klass, Parameter, Source, Statement, Statement.Try.Catch {
  }
}
