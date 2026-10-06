package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Intersects extends Rel {

  public Intersects(int nodeId, SqlToken left, SqlToken right) {
    super(nodeId, left, Operation.None, right);
  }

  @Override
  protected String doFormat(DatabaseVendor vendor, FormatOptions options) {
    return "st_intersects(" +
      left.format(vendor, options) + ", " +
      right.format(vendor, options) + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }
}
