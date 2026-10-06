package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class NotEqu extends Rel {
  public NotEqu(int nodeId, SqlToken left, SqlToken right) {
    super(nodeId, left, Operation.NotEq, right);
  }
}
