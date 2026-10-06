package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.*;

public class Group extends Expression {
  public Group(int nodeId, SqlToken left) {
    super(nodeId, left, Operation.None, null);
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String string = this.left.format(vendor, options, logicalContext);

    if (string != null) {
      SqlToken token = left;

      if (token instanceof And || token instanceof Rel || token instanceof SqlConst || token instanceof Group) {
        return string;
      }

      return "(" + string + ")";
    }
    return null;
  }

  @Override
  public FieldType type() {
    return left.type();
  }
}
