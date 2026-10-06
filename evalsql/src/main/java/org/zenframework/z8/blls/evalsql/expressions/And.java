package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.functions.If;

public class And extends Expression {
  static public String sqlAnd = "and";

//	static public And fromList(Collection<SqlToken> tokens) {
//		if(tokens.size() < 2)
//			throw new RuntimeException("And.fromList: incorrect number of parameters");
//
//		Iterator<SqlToken> iterator = tokens.iterator();
//		And and = new And(iterator.next(), iterator.next());
//
//		while(iterator.hasNext())
//			and = new And(and, iterator.next());
//
//		return and;
//	}

  public And(int nodeId, SqlToken left, SqlToken right) {
    super(nodeId, left, Operation.And, right);
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    if (logicalContext) {
      SqlToken left = this.left instanceof SqlConst.Bool b && b.getValue() ? null : (this.left instanceof Group ? this.left : new Group(nodeId(), this.left));
      SqlToken right = this.right instanceof SqlConst.Bool b && b.getValue() ? null : (this.right instanceof Group ? this.right : new Group(nodeId(), this.right));

      String leftText = left == null ? null : left.format(vendor, options, true);
      String rightText = right == null ? null : right.format(vendor, options, true);

      if (leftText != null && rightText != null)
        return leftText + " AND " + rightText;
      else if (leftText != null)
        return leftText;
      else return rightText;
    } else {
      SqlToken token = new If(nodeId(), this, new SqlConst.Integer(nodeId(), 1), new SqlConst.Integer(nodeId(), 0));
      return token.format(vendor, options);
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }
}
