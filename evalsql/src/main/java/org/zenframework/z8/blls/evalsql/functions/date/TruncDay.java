package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.*;

import java.time.Duration;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TruncDay extends SqlToken {
  private SqlToken date;

  public TruncDay(int nodeId, SqlToken date) {
    super(nodeId);
    this.date = date;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		date.collectFields(fields);
//	}

//	private date getConst() {
//		return date.isConst() && date.isDate() ? date.date() : null;
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
//        date date = getConst();
    if (date instanceof SqlConst.Date d) {
      var c = new GregorianCalendar();
      c.setTimeInMillis(d.getMilliseconds());
      c = new GregorianCalendar(c.get(GregorianCalendar.YEAR), c.get(GregorianCalendar.MONTH), c.get(GregorianCalendar.DAY_OF_MONTH));
      date = new SqlConst.Date(nodeId(), c.getTimeInMillis());
      return date.format(vendor, options);
    }

    String field = this.date.format(vendor, options);
    return "(" + field + " - MOD(" + field + " + (" + TimeZone.getDefault().getRawOffset() + "), " + Duration.ofDays(1).toMillis() + "))";
  }

  @Override
  public FieldType type() {
    return FieldType.Date;
  }
}
