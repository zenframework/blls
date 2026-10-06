package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class RTrim extends StringFunction {
  private final SqlToken string;

  public RTrim(int nodeId, SqlToken string) {
    super(nodeId);
    this.string = string;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return "RTRIM(" + string.format(vendor, options) + ")";
  }
}
