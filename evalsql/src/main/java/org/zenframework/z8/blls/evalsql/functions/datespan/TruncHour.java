package org.zenframework.z8.blls.evalsql.functions.datespan;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.expressions.Mul;
import org.zenframework.z8.blls.evalsql.expressions.Operation;

import java.time.Duration;

public class TruncHour extends SqlToken {
  private final SqlToken span;

  public TruncHour(int nodeId, SqlToken span) {
    super(nodeId);
    this.span = span;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		span.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return new Mul(nodeId(), new Days(nodeId(), span), Operation.Mul, new SqlConst.Integer(nodeId(), Duration.ofDays(1).toMillis())).format(vendor, options);
  }

  @Override
  public FieldType type() {
    return FieldType.Datespan;
  }
}
