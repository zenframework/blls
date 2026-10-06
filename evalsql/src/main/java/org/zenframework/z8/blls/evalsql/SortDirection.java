package org.zenframework.z8.blls.evalsql;

public enum SortDirection {
  Asc,
  Desc;

  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }
}
