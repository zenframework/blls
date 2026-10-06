package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.functions.If;

public class Or extends Expression {
  static public String sqlOr = "or";

//	static public Or fromList(Collection<SqlToken> tokens) {
//		if(tokens.size() < 2)
//			throw new RuntimeException("Or.fromList: incorrect number of parameters");
//
//		Iterator<SqlToken> iterator = tokens.iterator();
//		Or or = new Or(iterator.next(), iterator.next());
//
//		while(iterator.hasNext())
//			or = new Or(or, iterator.next());
//
//		return or;
//	}

  public Or(int nodeId, SqlToken left, SqlToken right) {
    super(nodeId, left, Operation.Or, right);
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String result = left.format(vendor, options, true) + " OR " + right.format(vendor, options, true);

    if (!logicalContext) {
      SqlToken token = new If(nodeId(), new SqlStringToken(nodeId(), result, FieldType.Boolean), new SqlConst.Integer(nodeId(), 1), new SqlConst.Integer(nodeId(), 0));
      return token.format(vendor, options, false);
    }

    return result;
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }
}
