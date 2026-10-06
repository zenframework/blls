package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class GetJson extends StringFunction {
  private final SqlToken json;
  private final SqlToken element;

  public GetJson(int nodeId, SqlToken json, SqlToken element) {
    super(nodeId);
    this.json = json;
    this.element = element;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		json.collectFields(fields);
//		element.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    StringBuilder result = new StringBuilder();

    if (vendor == DatabaseVendor.Postgres)
      result.append("(").append(json.format(vendor, options)).append(")::json->>").append(element.format(vendor, options));
    else
      throw new UnknownDatabaseException();

    return result.toString();
  }
}
