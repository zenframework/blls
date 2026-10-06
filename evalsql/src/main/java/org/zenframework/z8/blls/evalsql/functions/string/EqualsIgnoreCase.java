package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.expressions.Operation;
import org.zenframework.z8.blls.evalsql.expressions.Rel;

public class EqualsIgnoreCase extends Rel {
  public EqualsIgnoreCase(int nodeId, SqlToken left, SqlToken right) {
    super(nodeId, new Lower(nodeId, left), Operation.Eq, new Lower(nodeId, right));
  }
}
