package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.SqlConst;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class IsNot extends Rel {
  public IsNot(int nodeId, SqlToken token) {
    super(nodeId, token, Operation.NotEq, new SqlConst.Bool(nodeId, true));
  }
}
