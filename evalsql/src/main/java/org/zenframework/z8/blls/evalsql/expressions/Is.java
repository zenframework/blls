package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.SqlConst;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Is extends Rel {
  public Is(int nodeId, SqlToken token) {
    super(nodeId, token, Operation.Eq, new SqlConst.Bool(nodeId, true));
  }
}
