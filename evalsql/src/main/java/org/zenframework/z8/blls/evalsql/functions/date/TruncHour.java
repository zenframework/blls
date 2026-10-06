package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class TruncHour extends SqlToken {
  private final SqlToken time;

  public TruncHour(int nodeId, SqlToken time) {
    super(nodeId);
    this.time = time;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		time.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String dt = time.format(vendor, options);
    return "(" + dt + " - MOD(" + dt + ", 3600000))";
  }

  @Override
  public FieldType type() {
    return FieldType.Date;
  }
}
