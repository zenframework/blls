package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.*;

import java.util.Collection;

public class InVector extends SqlToken {
  private In inToken = null;

  public InVector(int nodeId, SqlToken what, Collection<SqlConst> values) {
    super(nodeId);
    if (values.size() != 0) {
      inToken = new In(nodeId());
      inToken.setCondition(what);
      for (SqlConst i : values)
        inToken.addValues(i);
    }
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		if(inToken != null) {
//			inToken.collectFields(fields);
//		}
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    if (inToken != null) {
      return inToken.format(vendor, options, logicalContext);
    } else {
      return new SqlConst.Bool(nodeId(), false).format(vendor, options, logicalContext);
    }
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }
}
