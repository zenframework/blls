package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class LTrim extends StringFunction {
  private final SqlToken value;

  public LTrim(int nodeId, SqlToken value) {
    super(nodeId);
    this.value = value;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		value.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return "LTRIM(" + value.format(vendor, options) + ")";
  }
}
