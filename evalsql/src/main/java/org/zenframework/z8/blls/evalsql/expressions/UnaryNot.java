package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class UnaryNot extends Unary {
  public UnaryNot(int nodeId, SqlToken token) {
    super(nodeId, Operation.Not, token);
  }
}
