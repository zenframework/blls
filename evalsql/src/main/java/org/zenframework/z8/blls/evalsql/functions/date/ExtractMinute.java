package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class ExtractMinute extends ExtractDatePart {
  public ExtractMinute(int nodeId, SqlToken date) {
    super(nodeId, date, Minute);
  }
}
