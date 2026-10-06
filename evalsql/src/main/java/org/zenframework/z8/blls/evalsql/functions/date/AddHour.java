package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.time.Duration;

public class AddHour extends SqlToken {
  private final SqlToken date;
  private final SqlToken hours;

  public AddHour(int nodeId, SqlToken date, SqlToken hours) {
    super(nodeId);
    this.date = date;
    this.hours = hours;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		date.collectFields(fields);
//		hours.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String dt = date.format(vendor, options);
    return "(" + dt + " + " + hours.format(vendor, options) + " *" + Duration.ofHours(1).toMillis() + (vendor == DatabaseVendor.Postgres ? "::bigint" : "") + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Date;
  }
}
