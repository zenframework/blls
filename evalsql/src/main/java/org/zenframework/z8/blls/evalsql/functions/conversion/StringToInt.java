package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class StringToInt extends SqlToken {
  private final SqlToken string;

  public StringToInt(int nodeId, SqlToken string) {
    super(nodeId);
    this.string = string;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Oracle:
        return "TO_NUMBER(" + string.format(vendor, options) + ")";
      case SqlServer:
        return "CONVERT(int, " + string.format(vendor, options) + ")";
      case Postgres:
        return string.format(vendor, options) + "::bigint";
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
