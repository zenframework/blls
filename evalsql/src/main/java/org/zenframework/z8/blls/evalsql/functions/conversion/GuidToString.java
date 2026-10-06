package org.zenframework.z8.blls.evalsql.functions.conversion;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class GuidToString extends SqlToken {
  private final SqlToken guid;

  public GuidToString(int nodeId, SqlToken guid) {
    super(nodeId);
    this.guid = guid;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		guid.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String param = guid.format(vendor, options);

    switch (vendor) {
      case Postgres:
        return param + "::text";
      case Oracle:
        return "RAWTOHEX(" + param + ")";
      case SqlServer:
        return "Convert(nvarchar(40), " + param + ")";
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.String;
  }
}
