package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.functions.If;

public class Unary extends SqlToken {
  private final Operation operation;
  private final SqlToken token;

  public Unary(int nodeId, Operation operation, SqlToken token) {
    super(nodeId);
    this.operation = operation;
    this.token = token;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		if(token != null)
//			token.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
    switch (operation) {
      case Not: {
        SqlToken t = new UNOTToken();
        if (!logicalContext)
          t = new If(nodeId(), t, new SqlConst.Integer(nodeId(), 1), new SqlConst.Integer(nodeId(), 0));
        return t.format(vendor, options, logicalContext);
      }
      case Minus:
        return "(-" + token.format(vendor, options) + ")";
      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public FieldType type() {
    switch (operation) {
      case Not:
        return FieldType.Boolean;
      case Minus:
        return token.type();
      default:
        throw new UnsupportedOperationException();
    }
  }

  private class UNOTToken extends SqlToken {
    public UNOTToken() {
      super(Unary.this.nodeId());
    }

    @Override
    public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
      return "not(" + token.format(vendor, options, true) + ")";
    }

//		@Override
//		public void collectFields(Collection<IField> fields) {
//		}

    @Override
    public FieldType type() {
      return FieldType.Boolean;
    }
  }
}
