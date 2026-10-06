package org.zenframework.z8.blls.evalsql.functions.numeric;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Sign extends SqlToken {
  private final SqlToken value;

  public Sign(int nodeId, SqlToken value) {
    super(nodeId);
    this.value = value;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		value.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return "SIGN(" + value.format(vendor, options) + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
