package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.expressions.Equ;
import org.zenframework.z8.blls.evalsql.expressions.Group;
import org.zenframework.z8.blls.evalsql.expressions.Or;
import org.zenframework.z8.blls.evalsql.functions.IsNull;

public class IsEmpty extends SqlToken {
  private final SqlToken string;

  public IsEmpty(int nodeId, SqlToken string) {
    super(nodeId);
    this.string = string;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    SqlToken value = new Group(nodeId(), this.string);
    SqlToken t = new Group(nodeId(), new Or(nodeId(), new IsNull(nodeId(), value), new Equ(nodeId(), value, new SqlConst.String(nodeId(), ""))));
    return t.format(vendor, options, logicalContext);
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }
}
