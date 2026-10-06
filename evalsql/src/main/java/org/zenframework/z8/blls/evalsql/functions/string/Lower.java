package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.functions.conversion.ToString;

public class Lower extends StringFunction {
  private final SqlToken string;

  public Lower(int nodeId, SqlToken value) {
    super(nodeId);
    this.string = value;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return "LOWER(" + new ToString(nodeId(), string).format(vendor, options) + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.String;
  }
}
