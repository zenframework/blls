package org.zenframework.z8.blls.evalsql.functions.geometry;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.expressions.Add;
import org.zenframework.z8.blls.evalsql.expressions.Operation;

public class Element extends SqlToken {

  private final SqlToken geometry, n;

  public Element(int nodeId, SqlToken geometry, SqlToken n) {
    super(nodeId);
    this.geometry = geometry;
    this.n = n;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		geometry.collectFields(fields);
//		n.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    switch (vendor) {
      case Postgres:
        return "ST_GeometryN(" + geometry.format(vendor, options) + ", " +
          new Add(nodeId(), n, Operation.Add, new SqlConst.Integer(nodeId(), 1)).format(vendor, options) + ')';
      default:
        throw new UnknownDatabaseException();
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Geometry;
  }

}
