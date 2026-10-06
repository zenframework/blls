package org.zenframework.z8.blls.analyzer;

import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Operation;

import java.util.List;

public sealed interface Synthetic {
  record Parameter(Expression.Type type, String name) implements Synthetic {
  }

  record Method(Expression.Type source, String name, List<Parameter> parameters,
                Expression.Type result) implements Synthetic {
  }

  record Operator(Expression.Type source, Operation operation, List<Parameter> parameters,
                  Expression.Type result) implements Synthetic {
  }
}
