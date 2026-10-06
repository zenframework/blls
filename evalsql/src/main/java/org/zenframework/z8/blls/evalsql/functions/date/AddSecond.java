package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.time.Duration;

public class AddSecond extends SqlToken {
  private final SqlToken date;
  private final SqlToken seconds;

  public AddSecond(int nodeId, SqlToken date, SqlToken seconds) {
    super(nodeId);
    this.date = date;
    this.seconds = seconds;
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String dt = date.format(vendor, options);
    return "(" + dt + " + " + seconds.format(vendor, options) + " * " + Duration.ofSeconds(1).toMillis() + (vendor == DatabaseVendor.Postgres ? "::bigint" : "") + ")";
  }

  @Override
  public FieldType type() {
    return FieldType.Date;
  }
}
