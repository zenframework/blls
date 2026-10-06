package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.functions.numeric.Mod;

public class Mul extends Operator {
  public Mul(int nodeId, SqlToken l, Operation oper, SqlToken r) {
    super(nodeId, l, oper, r);
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (operation) {
      case Mul: {
        return left.format(vendor, options) + "*" + right.format(vendor, options);
      }

      case Div: {
        return left.format(vendor, options) + "/" + right.format(vendor, options);
      }

      case Mod: {
        return new Mod(nodeId(), left, right).format(vendor, options);
      }

      default:
        throw new UnsupportedOperationException();
    }
  }
}
