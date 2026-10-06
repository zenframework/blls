package org.zenframework.z8.blls.evalsql.functions.geometry;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class InterpolatePoint extends SqlToken {

  private final SqlToken geometry, fraction;

  public InterpolatePoint(int nodeId, SqlToken geometry, SqlToken fraction) {
    super(nodeId);
    this.geometry = geometry;
    this.fraction = fraction;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		geometry.collectFields(fields);
//		fraction.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
        return "ST_Line_Interpolate_Point(" + geometry.format(vendor, options) + ", " +
          fraction.format(vendor, options) + ')';
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Geometry;
  }

}
