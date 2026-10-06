package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.SqlToken;

public abstract class StringFunction extends SqlToken {
  protected StringFunction(int nodeId) {
    super(nodeId);
  }

  @Override
  public FieldType type() {
    return FieldType.String;
  }
}
