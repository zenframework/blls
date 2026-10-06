package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class In extends SqlToken {
  private final List<SqlToken> values = new LinkedList<SqlToken>();
  private SqlToken condition;

  public In(int nodeId) {
    super(nodeId);
  }

  public In(int nodeId, SqlToken condition, Collection<SqlToken> values) {
    super(nodeId);
    this.condition = condition;
    this.values.addAll(values);
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		if(condition != null) {
//			condition.collectFields(fields);
//		}
//
//		for(SqlToken value : values) {
//			value.collectFields(fields);
//		}
//	}

  public void setCondition(SqlToken condition) {
    this.condition = condition;
  }

  public void addValues(SqlToken values) {
    this.values.add(values);
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
    SqlToken t = new InToken();

    if (!logicalContext) {
      t = new If(nodeId(), t, new SqlConst.Integer(nodeId(), 1), new SqlConst.Integer(nodeId(), 0));
    }

    return t.format(vendor, options, logicalContext);
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }

  private class InToken extends SqlToken {
    public InToken() {
      super(In.this.nodeId());
    }

//		@Override
//		public void collectFields(Collection<IField> fields) {
//		}

    @Override
    public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
      String frm = "(" + condition.format(vendor, options) + " in (";
      Iterator<SqlToken> t = values.iterator();
      while (t.hasNext())
        frm += t.next().format(vendor, options) + (t.hasNext() ? ", " : "");
      frm += "))";
      return frm;
    }

    @Override
    public FieldType type() {
      return FieldType.Boolean;
    }
  }
}
