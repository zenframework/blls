package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.functions.conversion.ToString;

public class Array extends SqlToken {
  private final boolean distinct;
  private SqlToken token;

  public Array(int nodeId, SqlToken token, boolean distinct) {
    super(nodeId);
    this.token = token;
    this.distinct = distinct;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		token.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
    String result = "";
    switch (vendor) {
      case Oracle:
        result = "collect";
        break;
      case Postgres:
        result = options.isOrderBy() ? "array_agg" : "json_agg";
        if (token.type() == FieldType.Text)
          token = new ToString(nodeId(), token);
        if (distinct)
          token = new Distinct(nodeId(), token);
        break;
      default:
        throw new UnknownDatabaseException();
    }
    return result + '(' + token.format(vendor, options) + ')';
  }

  @Override
  public FieldType type() {
    return token.type();
  }
}
