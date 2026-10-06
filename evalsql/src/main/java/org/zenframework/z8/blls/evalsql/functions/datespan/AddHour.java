package org.zenframework.z8.blls.evalsql.functions.datespan;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.time.Duration;

public class AddHour extends SqlToken {
  private final SqlToken span;
  private final SqlToken hours;

  public AddHour(int nodeId, SqlToken span, SqlToken hours) {
    super(nodeId);
    this.span = span;
    this.hours = hours;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		span.collectFields(fields);
//		hours.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return span.format(vendor, options) + " + (" + hours.format(vendor, options) + " * " + Duration.ofHours(1).toMillis() + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Datespan;
  }
}
