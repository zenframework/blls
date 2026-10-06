package org.zenframework.z8.blls.evalsql;

import java.math.BigDecimal;
import java.util.UUID;

public abstract class SqlConst extends SqlToken {
  protected SqlConst(int nodeId) {
    super(nodeId);
  }

  @Override
  public boolean isConst() {
    return true;
  }

  public static final class Bool extends SqlConst {
    private final boolean value;

    public Bool(int nodeId, boolean value) {
      super(nodeId);
      this.value = value;
    }

    public boolean getValue() {
      return value;
    }

    @Override
    public java.lang.String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
      return (value ? "1" : "0") + (isLogicalContext ? "=1" : "");
    }

    @Override
    public FieldType type() {
      return FieldType.Boolean;
    }
  }

  public static final class Date extends SqlConst {
    private final long milliseconds;

    public Date(int nodeId, long milliseconds) {
      super(nodeId);
      this.milliseconds = milliseconds;
    }

    public long getMilliseconds() {
      return milliseconds;
    }

    @Override
    public java.lang.String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
      return java.lang.String.valueOf(milliseconds);
    }

    @Override
    public FieldType type() {
      return FieldType.Date;
    }
  }

  public static final class Datespan extends SqlConst {
    private final long milliseconds;

    public Datespan(int nodeId, long milliseconds) {
      super(nodeId);
      this.milliseconds = milliseconds;
    }

    @Override
    public java.lang.String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
      switch (vendor) {
        case SqlServer:
          return "(" + milliseconds + ")";
        default:
          return java.lang.String.valueOf(milliseconds);
      }
    }

    @Override
    public FieldType type() {
      return FieldType.Datespan;
    }
  }

  public static final class Decimal extends SqlConst {
    private static final int maxPrecision = 38;

    private final BigDecimal value;

    public Decimal(int nodeId, double value) {
      super(nodeId);
      this.value = BigDecimal.valueOf(value);
    }

    @Override
    public java.lang.String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
      switch (vendor) {
        case SqlServer: {
          int scale = (value.scale() == 0 ? 1 : value.scale());
          int precision = ((value.precision() + scale) > maxPrecision ? maxPrecision - scale : value.precision()) + scale;
          return "CONVERT([numeric](" + precision + "," + scale + "),(" + value.toPlainString() + "),(0))";
        }
        default:
          return toString();
      }
    }

    @Override
    public FieldType type() {
      return FieldType.Decimal;
    }
  }

  public static final class Guid extends SqlConst {
    private final UUID value;

    public Guid(int nodeId, UUID value) {
      super(nodeId);
      this.value = value;
    }

    @Override
    public java.lang.String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
      switch (vendor) {
        case Oracle:
          return "HEXTORAW('" + value.toString().replace("-", "").toUpperCase() + "')";
        case Postgres:
        case SqlServer:
        default:
          return "'" + value.toString().toUpperCase() + "'";
      }
    }

    @Override
    public FieldType type() {
      return FieldType.Guid;
    }
  }

  public static final class Integer extends SqlConst {
    private final long value;

    public Integer(int nodeId, long value) {
      super(nodeId);
      this.value = value;
    }

    @Override
    public java.lang.String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
      return java.lang.String.valueOf(value);
    }

    @Override
    public FieldType type() {
      return FieldType.Integer;
    }
  }

  public static final class String extends SqlConst {
    private final java.lang.String value;

    public String(int nodeId, java.lang.String value) {
      super(nodeId);
      this.value = value;
    }

    @Override
    public java.lang.String format(DatabaseVendor vendor, FormatOptions options, boolean isLogicalContext) {
      java.lang.String string = (value == null ? "" : value).replaceAll("'", "''");

      if (vendor == DatabaseVendor.SqlServer || vendor == DatabaseVendor.Oracle)
        return "N'" + string + "'";

      return "'" + string + "'";
    }

    @Override
    public FieldType type() {
      return FieldType.String;
    }
  }

//	@Override
//	public void collectFields(Collection<IField> fields) {
//	}
}
