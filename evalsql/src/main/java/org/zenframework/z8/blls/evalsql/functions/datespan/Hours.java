package org.zenframework.z8.blls.evalsql.functions.datespan;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.functions.numeric.Mod;
import org.zenframework.z8.blls.evalsql.functions.numeric.Round;

public class Hours extends SqlToken {
  private final SqlToken span;

  public Hours(int nodeId, SqlToken span) {
    super(nodeId);
    this.span = span;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		span.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return new Round(nodeId(), new Mod(nodeId(), new TotalHours(nodeId(), span), new SqlConst.Integer(nodeId(), 24)), null).format(vendor, options);
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
