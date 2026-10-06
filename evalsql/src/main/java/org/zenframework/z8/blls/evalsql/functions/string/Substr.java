package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlConst;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.expressions.Add;
import org.zenframework.z8.blls.evalsql.expressions.Operation;

public class Substr extends StringFunction {
  private final SqlToken string;
  private final SqlToken position;
  private final SqlToken length;

  public Substr(int nodeId, SqlToken string, SqlToken position) {
    this(nodeId, string, position, null);
  }

  public Substr(int nodeId, SqlToken string, SqlToken position, SqlToken length) {
    super(nodeId);
    this.string = string;
    this.position = new Add(nodeId(), position, Operation.Add, new SqlConst.Integer(nodeId(), 1));
    this.length = length != null ? length : new Length(nodeId(), string);
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//		position.collectFields(fields);
//		length.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String result = "";

    if (vendor == DatabaseVendor.Oracle || vendor == DatabaseVendor.Postgres)
      result = "substr(";
    else if (vendor == DatabaseVendor.SqlServer)
      result = "subString(";
    else
      throw new UnknownDatabaseException();

    result += string.format(vendor, options) + "," + position.format(vendor, options) + "," + length.format(vendor, options) + ")";

    return result;
  }
}
