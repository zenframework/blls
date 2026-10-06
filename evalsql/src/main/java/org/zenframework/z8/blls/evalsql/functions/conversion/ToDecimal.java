package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class ToDecimal extends SqlToken {
  private final SqlToken token;

  public ToDecimal(int nodeId, SqlToken token) {
    super(nodeId);
    this.token = token;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		token.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Oracle:
        return "TO_NUMBER(" + token.format(vendor, options) + ", '999999999999999999999999.9999999999')";
      case SqlServer:
        return "CONVERT(numeric(36,14)," + token.format(vendor, options) + ")";
      case Postgres:
        // return "FLOAT(" + param1.format(vendor, options) + ")";
        return "CAST(" + token.format(vendor, options) + " AS FLOAT)";
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Decimal;
  }
}
