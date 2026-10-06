package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class DateToString extends SqlToken {
  private final SqlToken date;

  public DateToString(int nodeId, SqlToken date) {
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
        return "TO_CHAR(TO_TIMESTAMP(" + date.format(vendor, options) + " / 1000), 'DD.MM.YYYY HH24:MI:SS')";
      case Oracle:
        throw new UnsupportedOperationException();
//			return "TO_NCHAR(" + date.format(vendor, options) + ", 'DD/MM/YYYY HH24:MI:SS')";
      case SqlServer:
        return "Convert(nvarchar, " + date.format(vendor, options) + ", 103) + ' ' + Convert(nvarchar(8), " + date.format(vendor, options) + ", 114)";
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.String;
  }
}
