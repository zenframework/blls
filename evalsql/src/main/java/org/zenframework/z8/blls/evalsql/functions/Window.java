package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.functions.conversion.ToString;

public class Window extends SqlToken {
  private final SqlToken token;
  private final String name;

  protected Window(int nodeId, SqlToken token, String name) {
    super(nodeId);
    this.token = token;
    this.name = name;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		token.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
    String result = options.isAggregationEnabled() ? name + "(" : "";

    FieldType type = token.type();

    options.disableAggregation();

    SqlToken token = this.token;

    switch (vendor) {
      case Postgres:
      case Oracle:
        if (type == FieldType.Guid || type == FieldType.Text || type == FieldType.Attachments || type == FieldType.File)
          token = new ToString(nodeId(), token);
        break;
      default:
    }

    result += token.format(vendor, options);
    options.enableAggregation();

    result += options.isAggregationEnabled() ? ")" : "";

    switch (vendor) {
      case Postgres:
        if (type == FieldType.Guid)
          result += "::uuid";
        break;
      case Oracle:
        if (type == FieldType.Guid)
          result = "HEXTORAW(" + result + ")";
        break;
      default:
    }

    return result;
  }

  @Override
  public FieldType type() {
    return token.type();
  }
}
