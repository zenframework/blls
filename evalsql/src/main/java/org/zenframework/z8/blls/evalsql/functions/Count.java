package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class Count extends Window {
  public Count(int nodeId, SqlToken token) {
    super(nodeId, token, "count");
  }
}
