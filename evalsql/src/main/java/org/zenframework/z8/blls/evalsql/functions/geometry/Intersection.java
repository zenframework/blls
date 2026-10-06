package org.zenframework.z8.blls.evalsql.functions.geometry;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Intersection extends SqlToken {

  private final SqlToken geometry1, geometry2;

  public Intersection(int nodeId, SqlToken geometry1, SqlToken geometry2) {
    super(nodeId);
    this.geometry1 = geometry1;
    this.geometry2 = geometry2;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		geometry1.collectFields(fields);
//		geometry2.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
        return "ST_Intersection(" + geometry1.format(vendor, options) + ", " +
          geometry2.format(vendor, options) + ')';
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Geometry;
  }

}
