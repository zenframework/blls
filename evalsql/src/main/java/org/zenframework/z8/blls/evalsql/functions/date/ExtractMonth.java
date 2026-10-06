package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class ExtractMonth extends ExtractDatePart {
  public ExtractMonth(int nodeId, SqlToken date) {
    super(nodeId, date, Month);
  }
}
