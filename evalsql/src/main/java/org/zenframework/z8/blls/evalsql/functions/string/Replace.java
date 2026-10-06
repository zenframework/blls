package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Replace extends StringFunction {
  private final SqlToken string;
  private final SqlToken pattern;
  private final SqlToken replacement;

  public Replace(int nodeId, SqlToken string, SqlToken pattern, SqlToken replacement) {
    super(nodeId);
    this.string = string;
    this.pattern = pattern;
    this.replacement = replacement;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//		pattern.collectFields(fields);
//		replacement.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Oracle:
      case SqlServer:
      case Postgres:
        return "replace(" + string.format(vendor, options) + ", " + pattern.format(vendor, options) + ", " + replacement.format(vendor, options) + ")";
      default:
        throw new UnknownDatabaseException();
    }
  }
}
