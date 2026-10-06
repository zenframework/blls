package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.functions.Nvl;

public class RegIndexOf extends SqlToken {
  private final SqlToken string;
  private final SqlToken pattern;

  public RegIndexOf(int nodeId, SqlToken string, SqlToken pattern) {
    super(nodeId);
    this.string = string;
    this.pattern = pattern;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//		pattern.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String result = null;

    if (vendor == DatabaseVendor.Oracle) {
      result = "RegExp_instr(" + pattern.format(vendor, options) + ", " + string.format(vendor, options) + ") - 1";
    } else if (vendor == DatabaseVendor.SqlServer) {
      result = "patIndex(" + pattern.format(vendor, options) + ", " + string.format(vendor, options) + ") - 1";
    } else
      throw new UnknownDatabaseException();

    return (new Nvl(nodeId(), new SqlStringToken(nodeId(), result, FieldType.Integer), new SqlConst.Integer(nodeId(), -1))).format(vendor, options);
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
