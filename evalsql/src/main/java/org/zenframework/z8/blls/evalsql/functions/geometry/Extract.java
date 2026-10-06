package org.zenframework.z8.blls.evalsql.functions.geometry;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Extract extends SqlToken {

  private final SqlToken geometry, type;

  public Extract(int nodeId, SqlToken geometry, SqlToken type) {
    super(nodeId);
    this.geometry = geometry;
    this.type = type;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		geometry.collectFields(fields);
//		type.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
        return "ST_CollectionExtract(" + geometry.format(vendor, options) +
          ", " + type.format(vendor, options) + ')';
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Geometry;
  }

}
