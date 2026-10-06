package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Concat extends SqlToken {
  private final SqlToken token;

  public Concat(int nodeId, SqlToken token) {
    super(nodeId);
    this.token = token;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		token.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
    StringBuilder str = new StringBuilder();
    switch (vendor) {
      case Postgres:
        str.append("string_agg");
        break;
      default:
        throw new UnknownDatabaseException();
    }
    return str.append('(').append(token.format(vendor, options)).append(",',')").toString();
  }

  @Override
  public FieldType type() {
    return FieldType.String;
  }
}
