package org.zenframework.z8.blls.evalsql;

import org.zenframework.z8.blls.evalsql.functions.In;

public class SqlIn extends SqlToken {
  private final In inToken = new In(nodeId());

  public SqlIn(int nodeId, SqlToken param1, SqlToken param2) {
    super(nodeId);
    inToken.setCondition(param1);
    inToken.addValues(param2);
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		inToken.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
    return inToken.format(vendor, options, logicalContext);
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }
}
