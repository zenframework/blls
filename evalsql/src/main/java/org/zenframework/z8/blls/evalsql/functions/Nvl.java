package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Nvl extends SqlToken {
  private final SqlToken first;
  private final SqlToken second;

  public Nvl(int nodeId, SqlToken first, SqlToken second) {
    super(nodeId);
    this.first = first;
    this.second = second;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		first.collectFields(fields);
//		second.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Oracle:
        return "nvl(" + first.format(vendor, options) + "," + second.format(vendor, options, logicalContext) + ")";
      case SqlServer:
        return "isNull(" + first.format(vendor, options) + "," + second.format(vendor, options, logicalContext) + ")";
      case Postgres:
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return first.type();
  }
}
