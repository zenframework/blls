package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.SqlToken;

abstract public class Expression extends SqlToken {
  protected Operation operation;
  protected SqlToken left;
  protected SqlToken right;

  public Expression(int nodeId, SqlToken left, Operation operation, SqlToken right) {
    super(nodeId);
    this.left = left;
    this.operation = operation;
    this.right = right;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		if(left != null)
//			left.collectFields(fields);
//
//		if(right != null)
//			right.collectFields(fields);
//	}
}
