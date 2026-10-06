package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.functions.Nvl;

public class Length extends SqlToken {
  private final SqlToken string;

  public Length(int nodeId, SqlToken string) {
    super(nodeId);
    this.string = string;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String length = string.format(vendor, options);

    switch (vendor) {
      case Oracle:
        return new Nvl(nodeId(), new SqlStringToken(nodeId(), "length(" + length + ")", FieldType.Integer), new SqlConst.Integer(nodeId(), 0)).format(vendor, options);
      case Postgres:
        return new SqlStringToken(nodeId(), "length(" + length + ")", FieldType.Integer).format(vendor, options);
      case SqlServer:
        return new Nvl(nodeId(), new SqlStringToken(nodeId(), "len(" + length + ")", FieldType.Integer), new SqlConst.Integer(nodeId(), 0)).format(vendor, options);
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
