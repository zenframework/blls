package org.zenframework.z8.blls.evalsql;

public class SqlStringToken extends SqlToken {
  private final FieldType type;
  private final String value;

  public SqlStringToken(int nodeId, String value, FieldType type) {
    super(nodeId);
    this.type = type;
    this.value = value;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
    return value;
  }

  @Override
  public FieldType type() {
    return type; //FieldType.Null;
  }
}
