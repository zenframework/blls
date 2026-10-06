package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlConst;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class RPad extends StringFunction {
  private final SqlToken string;
  private final SqlToken length;
  private SqlToken pad;

  public RPad(int nodeId, SqlToken string, SqlToken length, SqlToken pad) {
    super(nodeId);
    this.string = string;
    this.length = length;
    this.pad = pad;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//		length.collectFields(fields);
//		pad.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    pad = (pad == null ? new SqlConst.String(nodeId(), " ") : pad);
    switch (vendor) {
      case Oracle:
        return "RPAD(" + string.format(vendor, options) + "," + length.format(vendor, options) + "," + pad.format(vendor, options) + ")";
      case SqlServer:
        return "LEFT(" + string.format(vendor, options) + "+REPLICATE(" + pad.format(vendor, options) + "," + length.format(vendor, options) + ")" + "," + length.format(vendor, options) + ")";
      case Postgres:
        return "rpad(" + string.format(vendor, options) + ", " + length.format(vendor, options) + ", " + pad.format(vendor, options) + ")";
      default:
        throw new UnknownDatabaseException();
    }
  }
}
