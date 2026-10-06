package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class IntToDecimal extends SqlToken {
  private final SqlToken token;

  public IntToDecimal(int nodeId, SqlToken token) {
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
        return token.format(vendor, options);
      case SqlServer:
      case Postgres:
        return new ToDecimal(nodeId(), token).format(vendor, options);
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Decimal;
  }
}
