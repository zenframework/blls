package org.zenframework.z8.blls.evalsql.functions.string;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;
import org.zenframework.z8.blls.evalsql.functions.conversion.ToString;

public class Like extends SqlToken {
  private final SqlToken string;
  private final SqlToken like;
  private final SqlToken escape;

  public Like(int nodeId, SqlToken string, SqlToken like) {
    this(nodeId, string, like, null);
  }

  public Like(int nodeId, SqlToken string, SqlToken like, SqlToken escape) {
    super(nodeId);
    this.string = string;
    this.like = like;
    this.escape = escape;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		string.collectFields(fields);
//		like.collectFields(fields);
//		if(escape != null)
//			escape.collectFields(fields);
//	}

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    return new ToString(nodeId(), string).format(vendor, options) + " LIKE " + new ToString(nodeId(), like).format(vendor, options) + (escape == null ? "" : " ESCAPE " + new ToString(nodeId(), escape).format(vendor, options));
  }
}
