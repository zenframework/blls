package org.zenframework.z8.blls.evalsql.functions.datespan;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.time.Duration;

public class AddMinute extends SqlToken {
  private final SqlToken span;
  private final SqlToken minutes;

  public AddMinute(int nodeId, SqlToken span, SqlToken minutes) {
    super(nodeId);
    this.span = span;
    this.minutes = minutes;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		span.collectFields(fields);
//		minutes.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return span.format(vendor, options) + " + (" + minutes.format(vendor, options) + " * " + Duration.ofMinutes(1).toMillis() + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Datespan;
  }
}
