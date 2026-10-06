package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class DatespanToString extends SqlToken {
  private final SqlToken datespan;

  public DatespanToString(int nodeId, SqlToken datespan) {
    super(nodeId);
    this.datespan = datespan;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		datespan.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return datespan.format(vendor, options);
  }

  @Override
  public FieldType type() {
    return FieldType.String;
  }
}
