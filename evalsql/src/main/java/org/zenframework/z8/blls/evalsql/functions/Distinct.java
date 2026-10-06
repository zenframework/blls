package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Distinct extends SqlToken {
  private final SqlToken token;

  public Distinct(int nodeId, SqlToken token) {
    super(nodeId);
    this.token = token;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		token.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
    StringBuilder result = new StringBuilder(1024);
    if (vendor == DatabaseVendor.Postgres)
      result.append("distinct ").append(token.format(vendor, options, isLogicalContext));
    else
      throw new UnknownDatabaseException();
    return result.toString();
  }

  @Override
  public FieldType type() {
    return token.type();
  }

}
