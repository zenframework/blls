package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class ToBytes extends SqlToken {
  private final SqlToken value;

  public ToBytes(int nodeId, SqlToken value) {
    super(nodeId);
    this.value = value;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		value.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
        return "CONVERT_TO(" + value.format(vendor, options) + ", 'UTF8')";
      case Oracle:
      case SqlServer:
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Binary;
  }
}
