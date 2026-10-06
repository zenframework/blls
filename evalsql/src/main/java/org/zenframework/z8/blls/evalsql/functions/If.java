package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.DatabaseVendor;
import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.FormatOptions;
import org.zenframework.z8.blls.evalsql.SqlToken;

public class If extends SqlToken {
  private final SqlToken condition;
  private final SqlToken trueToken;
  private final SqlToken falseToken;

  public If(int nodeId, SqlToken condition, SqlToken trueToken, SqlToken falseToken) {
    super(nodeId);
    this.condition = condition;
    this.trueToken = trueToken;
    this.falseToken = falseToken;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		condition.collectFields(fields);
//		trueToken.collectFields(fields);
//		falseToken.collectFields(fields);
//	}

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    CaseToken CaseToken = new CaseToken(nodeId());
    CaseToken.addWhen(condition, trueToken);
    CaseToken.setElse(falseToken);

    String result = CaseToken.format(vendor, options);

    if (logicalContext)
      result = "(" + result + ")=1"; // Where

    return result;
  }

  @Override
  public FieldType type() {
    return trueToken.type();
  }
}
