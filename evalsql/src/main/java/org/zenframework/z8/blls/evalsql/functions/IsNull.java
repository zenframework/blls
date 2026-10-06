package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.*;

public class IsNull extends SqlToken {
  private final SqlToken token;

  public IsNull(int nodeId, SqlToken token) {
    super(nodeId);
    this.token = token;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		token.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
    SqlToken t = new IsNullToken();
    if (!logicalContext)
      t = new If(nodeId(), t, new SqlConst.Integer(nodeId(), 1), new SqlConst.Integer(nodeId(), 0));

    return t.format(vendor, options, true);
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }

  private class IsNullToken extends SqlToken {
    private IsNullToken() {
      super(IsNull.this.nodeId());
    }

//		@Override
//		public void collectFields(Collection<IField> fields) {
//		}

    @Override
    public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
      return token.format(vendor, options) + " is null";
    }

    @Override
    public FieldType type() {
      return FieldType.Boolean;
    }
  }
}
