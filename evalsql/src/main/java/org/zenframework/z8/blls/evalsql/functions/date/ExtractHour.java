package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class ExtractHour extends ExtractDatePart {
  public ExtractHour(int nodeId, SqlToken date) {
    super(nodeId, date, Hour);
  }
}
