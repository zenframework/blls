package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.time.Duration;

public class AddDay extends SqlToken {
  private final SqlToken date;
  private final SqlToken days;

  public AddDay(int nodeId, SqlToken date, SqlToken days) {
    super(nodeId);
    this.date = date;
    this.days = days;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		date.collectFields(fields);
//		days.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String dt = date.format(vendor, options);
    return "(" + dt + " + " + days.format(vendor, options) + " * " + Duration.ofDays(1).toMillis() + (vendor == DatabaseVendor.Postgres ? "::bigint" : "") + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Date;
  }
}
