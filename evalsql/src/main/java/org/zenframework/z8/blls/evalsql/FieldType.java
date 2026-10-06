package org.zenframework.z8.blls.evalsql;

public enum FieldType {
  None,
  Null,

  Attachments,
  Binary,
  Boolean,
  Date,
  Datetime,
  Datespan,
  Decimal,
  File,
  Geometry,
  Guid,
  Integer,
  String,
  Text;

  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }
}
