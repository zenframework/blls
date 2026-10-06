package org.zenframework.z8.blls.evalsql.functions;

import org.zenframework.z8.blls.evalsql.FieldType;
import org.zenframework.z8.blls.evalsql.SqlToken;

import java.util.LinkedList;

public abstract class BaseCaseToken extends SqlToken {
  protected SqlToken condition = null;
  protected LinkedList<Pair<SqlToken, SqlToken>> whens = new LinkedList<Pair<SqlToken, SqlToken>>();
  protected SqlToken elseToken;

  public BaseCaseToken(int nodeId) {
    super(nodeId);
  }

  public void setCondition(SqlToken condition) {
    this.condition = condition;
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//		if(condition != null) {
//			condition.collectFields(fields);
//		}
//
//		for(Pair<SqlToken, SqlToken> pair : whens) {
//			pair.getLeft().collectFields(fields);
//			pair.getRight().collectFields(fields);
//		}
//
//		if(elseToken != null) {
//			elseToken.collectFields(fields);
//		}
//	}

  public void addWhen(SqlToken when, SqlToken result) {
    whens.add(new Pair<SqlToken, SqlToken>(when, result));
  }

  public void setElse(SqlToken condition) {
    elseToken = condition;
  }

  @Override
  public FieldType type() {
    return elseToken.type();
  }

  public class Pair<LEFT, RIGHT> {
    private LEFT left = null;
    private RIGHT right = null;

    public Pair() {
    }

    public Pair(LEFT left, RIGHT right) {
      this.left = left;
      this.right = right;
    }

    public Pair(Pair<LEFT, RIGHT> pair) {
      left = pair.left;
      right = pair.right;
    }

    public final LEFT getLeft() {
      return left;
    }

    public final void setLeft(LEFT left) {
      this.left = left;
    }

    public final RIGHT getRight() {
      return right;
    }

    public final void setRight(RIGHT right) {
      this.right = right;
    }
  }
}
