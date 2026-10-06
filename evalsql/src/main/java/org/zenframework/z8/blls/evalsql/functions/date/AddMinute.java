package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.time.Duration;

public class AddMinute extends SqlToken {
  private final SqlToken date;
  private final SqlToken minutes;

  public AddMinute(int nodeId, SqlToken date, SqlToken minutes) {
    super(nodeId);
    this.date = date;
    this.minutes = minutes;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		date.collectFields(fields);
//		minutes.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String dt = date.format(vendor, options);
    return "(" + dt + " + " + minutes.format(vendor, options) + " *" + Duration.ofMinutes(1).toMillis() + (vendor == DatabaseVendor.Postgres ? "::bigint" : "") + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Date;
  }
}
