package org.zenframework.z8.blls.evalsql.functions.numeric;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Ln extends SqlToken {
  private final SqlToken token;

  public Ln(int nodeId, SqlToken token) {
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
        return "LN(" + token.format(vendor, options) + ")";
      case SqlServer:
        return "LOG(" + token.format(vendor, options) + ")";
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Decimal;
  }
}
