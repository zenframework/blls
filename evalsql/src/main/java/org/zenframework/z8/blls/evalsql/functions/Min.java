package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class Min extends Window {
  public Min(int nodeId, SqlToken token) {
    super(nodeId, token, "min");
  }
}
