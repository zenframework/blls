package org.zenframework.z8.blls.evalsql;

import org.jspecify.annotations.NullMarked;

/// Не каждый SQL-запрос можно просчитать, т.к. в нём могут содержаться выражения, определяемые во время исполнения
@NullMarked
public final class Uneval extends SqlToken {
  private final FieldType type;

  public Uneval(int nodeId, FieldType type) {
    super(nodeId);
    this.type = type;
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
    throw new Exception();
  }

  @Override
  public FieldType type() {
    return type;
  }

  public final class Exception extends RuntimeException {
    public SqlToken getToken() {
      return Uneval.this;
    }
  }
}
