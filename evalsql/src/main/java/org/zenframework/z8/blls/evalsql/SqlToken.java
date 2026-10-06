package org.zenframework.z8.blls.evalsql;

abstract public class SqlToken {
//	private Collection<IField> usedFields = null;

  private final int nodeId;

  protected SqlToken(int nodeId) {
    this.nodeId = nodeId;
  }

  public int nodeId() {
    return nodeId;
  }

  public String format(DatabaseVendor vendor, FormatOptions options) {
    return format(vendor, options, false);
  }

  abstract public String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext);

//	abstract public void collectFields(Collection<IField> fields);

  public boolean isConst() {
    return false;
  }

//	public Collection<IField> getUsedFields() {
//		if(usedFields == null) {
//			usedFields = new HashSet<IField>();
//			collectFields(usedFields);
//		}
//		return usedFields;
//	}

  public boolean isNumeric() {
    return isInteger() || isDecimal();
  }

  public boolean isInteger() {
    return type() == FieldType.Integer;
  }

  public boolean isDecimal() {
    return type() == FieldType.Decimal;
  }

  public boolean isDate() {
    return type() == FieldType.Date || type() == FieldType.Datetime;
  }

  public boolean isDatespan() {
    return type() == FieldType.Datespan;
  }

  abstract public FieldType type();

  public final class UnknownDatabaseException extends RuntimeException {
    public SqlToken getToken() {
      return SqlToken.this;
    }
  }
}
