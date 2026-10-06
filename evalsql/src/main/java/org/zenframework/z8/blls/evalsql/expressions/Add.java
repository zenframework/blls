package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.functions.string.Concat;

public class Add extends Operator {
  public Add(int nodeId, SqlToken left, Operation operation, SqlToken right) {
    super(nodeId, left, operation, right);
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (operation) {
      case Add:
        return left.type() == FieldType.String ? new Concat(nodeId(), left, right).format(vendor, options) : left.format(vendor, options) + " + " + right.format(vendor, options);
      case Sub:
        return left.format(vendor, options) + " - (" + right.format(vendor, options) + ")";
      default:
        throw new UnsupportedOperationException();
    }
  }
}
