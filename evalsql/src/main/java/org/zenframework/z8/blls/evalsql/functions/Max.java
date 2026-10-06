package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class Max extends Window {
  public Max(int nodeId, SqlToken token) {
    super(nodeId, token, "max");
  }
}
