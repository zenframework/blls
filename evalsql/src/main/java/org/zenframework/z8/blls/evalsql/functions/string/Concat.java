package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Concat extends StringFunction {
  private final SqlToken left;
  private final SqlToken right;

  public Concat(int nodeId, SqlToken left, SqlToken right) {
    super(nodeId);
    this.left = left;
    this.right = right;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		left.collectFields(fields);
//		right.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Oracle:
      case Postgres:
        return "Concat(" + left.format(vendor, options) + ", " + right.format(vendor, options) + ")";
      case SqlServer:
        return "(" + left.format(vendor, options) + "+" + right.format(vendor, options) + ")";
      default:
        throw new UnknownDatabaseException();
    }
  }
}
