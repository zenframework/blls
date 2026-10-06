package org.zenframework.z8.blls.evalsql.functions.date;

import org.zenframework.z8.blls.evalsql.SqlToken;

public class ExtractYear extends ExtractDatePart {
  public ExtractYear(int nodeId, SqlToken date) {
    super(nodeId, date, Year);
  }
}
