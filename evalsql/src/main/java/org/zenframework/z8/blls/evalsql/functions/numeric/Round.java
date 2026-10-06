package org.zenframework.z8.blls.evalsql.functions.numeric;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Round extends SqlToken {
  private final SqlToken number;
  private final SqlToken digits;

  public Round(int nodeId, SqlToken number, SqlToken digits) {
    super(nodeId);
    this.number = number;
    this.digits = digits;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		number.collectFields(fields);
//		digits.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Oracle:
      case Postgres:
        return "ROUND(CAST(" + number.format(vendor, options) + " AS numeric), " + (digits != null ? digits.format(vendor, options) : "0") + ")";
      case SqlServer:
        return "ROUND(" + number.format(vendor, options) + ", " + (digits != null ? digits.format(vendor, options) : "0") + ", 0)";
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return number.type();
  }
}
