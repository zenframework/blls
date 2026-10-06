package org.zenframework.z8.blls.evalsql.functions.numeric;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Exp extends SqlToken {
  private final SqlToken token;

  public Exp(int nodeId, SqlToken token) {
    super(nodeId);
    this.token = token;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		token.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return "EXP(" + token.format(vendor, options) + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Decimal;
  }
}
