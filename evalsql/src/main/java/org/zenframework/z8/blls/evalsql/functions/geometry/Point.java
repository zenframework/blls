package org.zenframework.z8.blls.evalsql.functions.geometry;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class Point extends SqlToken {

  private final SqlToken x, y, srs;

  public Point(int nodeId, SqlToken x, SqlToken y, SqlToken srs) {
    super(nodeId);
    this.x = x;
    this.y = y;
    this.srs = srs;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		x.collectFields(fields);
//		y.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
        return "ST_SetSRID(ST_Point(" + x.format(vendor, options) + ", " +
          y.format(vendor, options) + "), " + srs.format(vendor, options) + ')';
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Geometry;
  }

}
