package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class RegLike extends SqlToken {
  private final SqlToken string;
  private final SqlToken pattern;

  public RegLike(int nodeId, SqlToken string, SqlToken pattern) {
    super(nodeId);
    this.string = string;
    this.pattern = pattern;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//		pattern.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    if (vendor == DatabaseVendor.Postgres)
      return string.format(vendor, options) + " ~ " + pattern.format(vendor, options);
    else
      throw new UnknownDatabaseException();
  }

  @Override
  public FieldType type() {
    return FieldType.Integer;
  }
}
