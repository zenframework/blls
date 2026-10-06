package org.zenframework.z8.blls.evalsql.functions.numeric;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Power extends SqlToken {
  private final SqlToken value;
  private final SqlToken power;

  public Power(int nodeId, SqlToken value, SqlToken power) {
    super(nodeId);
    this.value = value;
    this.power = power;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		value.collectFields(fields);
//		power.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return "POWER(" + value.format(vendor, options) + ", " + power.format(vendor, options) + ")";
  }

  @Override
  public FieldType type() {
    return value.type();
  }
}
