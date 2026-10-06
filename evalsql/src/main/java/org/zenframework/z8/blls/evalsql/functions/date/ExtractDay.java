package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class ExtractDay extends ExtractDatePart {
  public ExtractDay(int nodeId, SqlToken date) {
    super(nodeId, date, Day);
  }
}
