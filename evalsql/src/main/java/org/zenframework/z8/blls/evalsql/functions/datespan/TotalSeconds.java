package org.zenframework.z8.blls.evalsql.functions.datespan;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.expressions.Mul;
import org.zenframework.z8.blls.evalsql.expressions.Operation;
import org.zenframework.z8.blls.evalsql.functions.numeric.Round;

import java.time.Duration;

public class TotalSeconds extends SqlToken {
  private final SqlToken span;

  public TotalSeconds(int nodeId, SqlToken span) {
    super(nodeId);
    this.span = span;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		span.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return new Round(nodeId(), new Mul(nodeId(), span, Operation.Div, new SqlConst.Integer(nodeId(), Duration.ofSeconds(1).toMillis())), null).format(vendor, options);
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
