package org.zenframework.z8.blls.evalsql.expressions;

import org.zenframework.z8.blls.evalsql.*;
import org.zenframework.z8.blls.evalsql.functions.If;
import org.zenframework.z8.blls.evalsql.functions.date.TruncDay;

import java.util.GregorianCalendar;
import java.util.HashMap;

public class Rel extends Expression {
  final static HashMap<Operation, String> operations = new HashMap<Operation, String>();

  static {
    operations.put(Operation.Eq, "=");
    operations.put(Operation.NotEq, "<>");
    operations.put(Operation.LT, "<");
    operations.put(Operation.GT, ">");
    operations.put(Operation.LE, "<=");
    operations.put(Operation.GE, ">=");
  }

  public Rel(int nodeId, SqlToken left, Operation operation, SqlToken right) {
    super(nodeId, left, operation, right);
  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String rel = doFormat(vendor, options);

    if (!logicalContext)
      return new If(nodeId(), new SqlStringToken(nodeId(), rel, FieldType.Boolean), new SqlConst.Integer(nodeId(), 1), new SqlConst.Integer(nodeId(), 0)).format(vendor, options);

    return rel;
  }

  protected String doFormat(DatabaseVendor vendor, FormatOptions options) {
    if (left.type() == FieldType.Date && right.type() == FieldType.Date) {
      boolean leftConst = left.isConst();
      boolean rightConst = right.isConst();

      SqlToken result;
      if (leftConst && rightConst) {
        var lc = new GregorianCalendar();
        lc.setTimeInMillis(((SqlConst.Date) left).getMilliseconds());
        lc = new GregorianCalendar(lc.get(GregorianCalendar.YEAR), lc.get(GregorianCalendar.MONTH), lc.get(GregorianCalendar.DAY_OF_MONTH));

        var rc = new GregorianCalendar();
        rc.setTimeInMillis(((SqlConst.Date) right).getMilliseconds());
        rc = new GregorianCalendar(rc.get(GregorianCalendar.YEAR), rc.get(GregorianCalendar.MONTH), rc.get(GregorianCalendar.DAY_OF_MONTH));

        int compare = lc.compareTo(rc);// ? sql_bool.True : sql_bool.False;
        switch (operation) {
          case Eq:
            result = compare == 0 ? new SqlConst.Bool(nodeId(), true) : new SqlConst.Bool(nodeId(), false);
            break;
          case NotEq:
            result = compare != 0 ? new SqlConst.Bool(nodeId(), true) : new SqlConst.Bool(nodeId(), false);
            break;
          case LT:
            result = compare < 0 ? new SqlConst.Bool(nodeId(), true) : new SqlConst.Bool(nodeId(), false);
            break;
          case LE:
            result = compare <= 0 ? new SqlConst.Bool(nodeId(), true) : new SqlConst.Bool(nodeId(), false);
            break;
          case GT:
            result = compare > 0 ? new SqlConst.Bool(nodeId(), true) : new SqlConst.Bool(nodeId(), false);
            break;
          case GE:
            result = compare >= 0 ? new SqlConst.Bool(nodeId(), true) : new SqlConst.Bool(nodeId(), false);
            break;
          default:
            throw new UnsupportedOperationException();
        }
        return result.format(vendor, options);
      } else if (leftConst && !rightConst) {
        var lc = new GregorianCalendar();
        lc.setTimeInMillis(((SqlConst.Date) left).getMilliseconds());
        String field = right.format(vendor, options);

        var min = new GregorianCalendar();
        min.setTimeInMillis(-62135769600000L);

        var max = new GregorianCalendar();
        max.setTimeInMillis(95617584000000L);

        if (lc.equals(min) || lc.equals(max))
          return "(" + lc.getTimeInMillis() + " " + operations.get(operation) + " " + field + ")";

        lc = new GregorianCalendar(lc.get(GregorianCalendar.YEAR), lc.get(GregorianCalendar.MONTH), lc.get(GregorianCalendar.DAY_OF_MONTH));
        var lc1 = (GregorianCalendar) lc.clone();
        lc1.add(GregorianCalendar.DAY_OF_MONTH, 1);

        switch (operation) {
          case Eq:
            return "(" + lc.getTimeInMillis() + " <= " + field + " and " + field + " < " + lc1.getTimeInMillis() + ")";
          case NotEq:
            return "(" + field + " < " + lc.getTimeInMillis() + " or " + lc1.getTimeInMillis() + " <= " + field + ")";
          case LT:
            return "(" + lc1.getTimeInMillis() + " <= " + field + ")";
          case LE:
            return "(" + lc.getTimeInMillis() + " <= " + field + ")";
          case GT:
            return "(" + lc.getTimeInMillis() + " > " + field + ")";
          case GE:
            return "(" + lc1.getTimeInMillis() + " > " + field + ")";
          default:
            throw new UnsupportedOperationException();
        }
      } else if (!leftConst && rightConst) {
        var rc = new GregorianCalendar();
        rc.setTimeInMillis(((SqlConst.Date) right).getMilliseconds());
        String field = left.format(vendor, options);

        var min = new GregorianCalendar();
        min.setTimeInMillis(-62135769600000L);

        var max = new GregorianCalendar();
        max.setTimeInMillis(95617584000000L);

        if (rc.equals(min) || rc.equals(max))
          return "(" + field + " " + operations.get(operation) + " " + rc.getTimeInMillis() + ")";

        rc = new GregorianCalendar(rc.get(GregorianCalendar.YEAR), rc.get(GregorianCalendar.MONTH), rc.get(GregorianCalendar.DAY_OF_MONTH));
        var rc1 = (GregorianCalendar) rc.clone();
        rc1.add(GregorianCalendar.DAY_OF_MONTH, 1);

        switch (operation) {
          case Eq:
            return "(" + rc.getTimeInMillis() + " <= " + field + " and " + field + " < " + rc1.getTimeInMillis() + ")";
          case NotEq:
            return "(" + field + " < " + rc.getTimeInMillis() + " or " + rc1.getTimeInMillis() + " <= " + field + ")";
          case LT:
            return "(" + field + " < " + rc.getTimeInMillis() + ")";
          case LE:
            return "(" + field + " < " + rc1.getTimeInMillis() + ")";
          case GT:
            return "(" + field + " >= " + rc1.getTimeInMillis() + ")";
          case GE:
            return "(" + field + " >= " + rc.getTimeInMillis() + ")";
          default:
            throw new UnsupportedOperationException();
        }
      } else {
        left = new TruncDay(nodeId(), left);
        right = new TruncDay(nodeId(), right);
      }
    }

    return left.format(vendor, options) + operations.get(this.operation) + right.format(vendor, options);
  }

  @Override
  public FieldType type() {
    return FieldType.Boolean;
  }
}
