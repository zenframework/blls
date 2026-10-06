package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class Equ extends Rel {
  public Equ(int nodeId, SqlToken left, SqlToken right) {
    super(nodeId, left, Operation.Eq, right);
  }
}
