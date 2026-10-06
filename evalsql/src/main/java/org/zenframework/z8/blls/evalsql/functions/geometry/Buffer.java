package org.zenframework.z8.blls.evalsql.functions.geometry;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Buffer extends SqlToken {

  private final SqlToken geometry, radius;

  public Buffer(int nodeId, SqlToken geometry, SqlToken radius) {
    super(nodeId);
    this.geometry = geometry;
    this.radius = radius;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		geometry.collectFields(fields);
//		radius.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
        return "ST_Buffer(" + geometry.format(vendor, options) + ", " +
          radius.format(vendor, options) + ')';
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Geometry;
  }

}
