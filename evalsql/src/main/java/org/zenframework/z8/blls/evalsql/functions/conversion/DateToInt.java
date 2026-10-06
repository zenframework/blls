package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class DateToInt extends SqlToken {
  private final SqlToken date;

  public DateToInt(int nodeId, SqlToken date) {
    super(nodeId);
    this.date = date;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		date.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
      case Oracle:
        return date.format(vendor, options);
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
