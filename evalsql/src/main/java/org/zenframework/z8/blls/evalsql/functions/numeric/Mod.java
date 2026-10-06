package org.zenframework.z8.blls.evalsql.functions.numeric;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Mod extends SqlToken {
  private final SqlToken value;
  private final SqlToken base;

  public Mod(int nodeId, SqlToken value, SqlToken base) {
    super(nodeId);
    this.value = value;
    this.base = base;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		value.collectFields(fields);
//		base.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Oracle:
        return "MOD(" + value.format(vendor, options) + ", " + base.format(vendor, options) + ")";
      case Postgres:
        return "mod(" + value.format(vendor, options) + ", " + base.format(vendor, options) + ")";
      case SqlServer:
        return value.format(vendor, options) + " % " + base.format(vendor, options);
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
