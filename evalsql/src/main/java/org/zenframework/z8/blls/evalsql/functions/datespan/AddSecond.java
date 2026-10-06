package org.zenframework.z8.blls.evalsql.functions.datespan;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.time.Duration;

public class AddSecond extends SqlToken {
  private final SqlToken span;
  private final SqlToken seconds;

  public AddSecond(int nodeId, SqlToken date, SqlToken seconds) {
    super(nodeId);
    this.span = date;
    this.seconds = seconds;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		span.collectFields(fields);
//		seconds.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return span.format(vendor, options) + " + (" + seconds.format(vendor, options) + " * " + Duration.ofSeconds(1).toMillis() + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Datespan;
  }
}
