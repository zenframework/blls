package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.SqlConst;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.functions.string.RegLike;

public class IsNumericString extends RegLike {

  private static final String PATTERN = "^[0-9\\.]+$";

  public IsNumericString(int nodeId, SqlToken string) {
    super(nodeId, string, new SqlConst.String(nodeId, PATTERN));
  }

}
