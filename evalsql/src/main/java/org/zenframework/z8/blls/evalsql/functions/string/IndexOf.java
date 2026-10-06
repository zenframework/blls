package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.expressions.Add;
import org.zenframework.z8.blls.evalsql.expressions.Operation;
import org.zenframework.z8.blls.evalsql.functions.Nvl;

public class IndexOf extends SqlToken {
  private final SqlToken what;
  private SqlToken where;
  private SqlToken from;

  public IndexOf(int nodeId, SqlToken what, SqlToken where, SqlToken from) {
    super(nodeId);
    this.what = what;
    this.where = where;
    this.from = from;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		this.what.collectFields(fields);
//		this.where.collectFields(fields);
//		if(from != null)
//			this.from.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String result;

    if (vendor == DatabaseVendor.Oracle) {
      from = from != null ? new Add(nodeId(), from, Operation.Add, new SqlConst.Integer(nodeId(), 1)) : new SqlConst.Integer(nodeId(), 1);
      result = "instr(" + what.format(vendor, options) + ", " + where.format(vendor, options) + ", " + from.format(vendor, options) + ") - 1";
    } else if (vendor == DatabaseVendor.SqlServer) {
      from = from != null ? from : new SqlConst.Integer(nodeId(), 0);
      result = "charIndex(" + what.format(vendor, options) + ", " + where.format(vendor, options) + ", " + from.format(vendor, options) + ") - 1";
    } else if (vendor == DatabaseVendor.Postgres) {
      where = from != null ? new Substr(nodeId(), where, from) : where;
      return "(position(" + what.format(vendor, options) + " in " + where.format(vendor, options) + ") - 1)";
    } else
      throw new UnknownDatabaseException();

    return (new Nvl(nodeId(), new SqlStringToken(nodeId(), result, FieldType.Integer), new SqlConst.Integer(nodeId(), -1))).format(vendor, options);
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
