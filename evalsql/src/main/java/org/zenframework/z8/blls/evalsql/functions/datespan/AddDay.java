package org.zenframework.z8.blls.evalsql.functions.datespan;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.time.Duration;

public class AddDay extends SqlToken {
  private final SqlToken span;
  private final SqlToken days;

  public AddDay(int nodeId, SqlToken span, SqlToken days) {
    super(nodeId);
    this.span = span;
    this.days = days;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		span.collectFields(fields);
//		days.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return span.format(vendor, options) + " + (" + days.format(vendor, options) + " * " + Duration.ofDays(1).toMillis() + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Datespan;
  }
}
