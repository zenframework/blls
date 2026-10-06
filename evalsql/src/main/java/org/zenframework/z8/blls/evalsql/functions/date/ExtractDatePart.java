package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class ExtractDatePart extends SqlToken {
  final static String Second = "second";
  final static String Minute = "minute";
  final static String Hour = "hour";

  final static String Day = "day";
  final static String Month = "month";
  final static String Year = "year";

  private final SqlToken date;
  private final String part;

  protected ExtractDatePart(int nodeId, SqlToken date, String part) {
    super(nodeId);
    this.date = date;
    this.part = part;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		date.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
        return "extract(" + part + " from to_timestamp(" + date.format(vendor, options) + " / 1000))";
      case Oracle:
      case SqlServer:
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
