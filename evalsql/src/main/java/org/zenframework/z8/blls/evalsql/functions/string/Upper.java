package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.functions.conversion.ToString;

public class Upper extends StringFunction {
  private final SqlToken string;

  public Upper(int nodeId, SqlToken string) {
    super(nodeId);
    this.string = string;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return "UPPER(" + new ToString(nodeId(), string).format(vendor, options) + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.String;
  }
}
