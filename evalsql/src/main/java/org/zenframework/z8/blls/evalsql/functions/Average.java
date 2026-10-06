package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class Average extends Window {
  public Average(int nodeId, SqlToken token) {
    super(nodeId, token, "avg");
  }
}
