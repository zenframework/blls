package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class RegReplace extends StringFunction {
  private final SqlToken string;
  private final SqlToken pattern;
  private final SqlToken replacement;
  private final SqlToken flags;

  public RegReplace(int nodeId, SqlToken string, SqlToken pattern, SqlToken replacement, SqlToken flags) {
    super(nodeId);
    this.string = string;
    this.pattern = pattern;
    this.replacement = replacement;
    this.flags = flags;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//		pattern.collectFields(fields);
//		replacement.collectFields(fields);
//		if (flags != null)
//			flags.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    StringBuilder str = new StringBuilder(1024);

    switch (vendor) {
      case Oracle:
      case SqlServer:
      case Postgres:
        str.append("regexp_replace(").append(string.format(vendor, options)).append(", ").append(pattern.format(vendor, options)).append(", ").append(replacement.format(vendor, options));
        if (flags != null)
          str.append(", ").append(flags.format(vendor, options));
        str.append(")");
        return str.toString();
      default:
        throw new UnknownDatabaseException();
    }
  }
}
