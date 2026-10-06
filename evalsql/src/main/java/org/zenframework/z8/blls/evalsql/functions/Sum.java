package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Sum extends Window {
  public Sum(int nodeId, SqlToken token) {
    super(nodeId, token, getSumFunction(token));
  }

  protected static String getSumFunction(SqlToken token) {
    return token.type() == FieldType.Geometry ? "ST_Union" : "sum";
  }
}
