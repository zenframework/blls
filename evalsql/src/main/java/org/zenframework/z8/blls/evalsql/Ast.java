package org.zenframework.z8.blls.evalsql;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.analyzer.MapInt;
import org.zenframework.z8.blls.analyzer.Resolver;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Node;
import org.zenframework.z8.blls.evalsql.expressions.Operation;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Iterator;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import static java.util.Arrays.stream;
import static org.zenframework.z8.blls.analyzer.Ast.only;

@NullMarked
public final class Ast extends org.zenframework.z8.blls.ast.Ast.Delegating {
  static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendLiteral('/')
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendLiteral('/')
    .appendValue(ChronoField.YEAR, 4)
    .optionalStart()
    .appendLiteral(' ')
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendLiteral(':')
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
    .appendLiteral(':')
    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
    .optionalEnd()
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
    .toFormatter();

  static final Pattern REGEX_DATESPAN = Pattern.compile("(\\d\\d) (\\d\\d):(\\d\\d):(\\d\\d)");

  static final List<Known> KNOWN_FIELDS = List.of(
    Known.$BinaryField,
    Known.$BoolField,
    Known.$DatespanField,
    Known.$DatetimeField,
    Known.$DecimalField,
    Known.$GeometryField,
    Known.$GuidField,
    Known.$IntField,
    Known.$StringField
  );
  static final List<Known.Method> KNOWN_METHODS = List.of(
    // BoolField IIF sql_bool sql_bool
    new Known.Method(Known.$BoolField, "IIF", Known.$sql_bool, Known.$sql_bool) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // BoolField IIF sql_date sql_date
    new Known.Method(Known.$BoolField, "IIF", Known.$sql_date, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // BoolField IIF sql_datespan sql_datespan
    new Known.Method(Known.$BoolField, "IIF", Known.$sql_datespan, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // BoolField IIF sql_decimal sql_decimal
    new Known.Method(Known.$BoolField, "IIF", Known.$sql_decimal, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // BoolField IIF sql_guid sql_guid
    new Known.Method(Known.$BoolField, "IIF", Known.$sql_guid, Known.$sql_guid) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // BoolField IIF sql_int sql_int
    new Known.Method(Known.$BoolField, "IIF", Known.$sql_int, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // BoolField IIF sql_string sql_string
    new Known.Method(Known.$BoolField, "IIF", Known.$sql_string, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // BoolField IIF sql_geometry sql_geometry
    new Known.Method(Known.$BoolField, "IIF", Known.$sql_geometry, Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // Sql IIF sql_bool sql_datespan sql_datespan
    new Known.Method.Static(Known.$Sql, "IIF", Known.$sql_bool, Known.$sql_datespan, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, @Nullable SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, arguments.next(), arguments.next(), arguments.next());
      }
    },
    // Sql IIF sql_bool sql_date sql_date
    new Known.Method.Static(Known.$Sql, "IIF", Known.$sql_bool, Known.$sql_date, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, @Nullable SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, arguments.next(), arguments.next(), arguments.next());
      }
    },
    // Sql IIF sql_bool sql_decimal sql_decimal
    new Known.Method.Static(Known.$Sql, "IIF", Known.$sql_bool, Known.$sql_decimal, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, @Nullable SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, arguments.next(), arguments.next(), arguments.next());
      }
    },
    // Sql IIF sql_bool sql_guid sql_guid
    new Known.Method.Static(Known.$Sql, "IIF", Known.$sql_bool, Known.$sql_guid, Known.$sql_guid) {
      @Override
      protected SqlToken run(int nodeId, @Nullable SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, arguments.next(), arguments.next(), arguments.next());
      }
    },
    // Sql IIF sql_bool sql_int sql_int
    new Known.Method.Static(Known.$Sql, "IIF", Known.$sql_bool, Known.$sql_int, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, @Nullable SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, arguments.next(), arguments.next(), arguments.next());
      }
    },
    // Sql IIF sql_bool sql_string sql_string
    new Known.Method.Static(Known.$Sql, "IIF", Known.$sql_bool, Known.$sql_string, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, @Nullable SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, arguments.next(), arguments.next(), arguments.next());
      }
    },
    // sql_bool IIF sql_bool sql_bool
    new Known.Method(Known.$sql_bool, "IIF", Known.$sql_bool, Known.$sql_bool) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_bool IIF sql_date sql_date
    new Known.Method(Known.$sql_bool, "IIF", Known.$sql_date, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_bool IIF sql_datespan sql_datespan
    new Known.Method(Known.$sql_bool, "IIF", Known.$sql_datespan, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_bool IIF sql_decimal sql_decimal
    new Known.Method(Known.$sql_bool, "IIF", Known.$sql_decimal, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_bool IIF sql_guid sql_guid
    new Known.Method(Known.$sql_bool, "IIF", Known.$sql_guid, Known.$sql_guid) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_bool IIF sql_int sql_int
    new Known.Method(Known.$sql_bool, "IIF", Known.$sql_int, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_bool IIF sql_string sql_string
    new Known.Method(Known.$sql_bool, "IIF", Known.$sql_string, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_bool IIF sql_geometry sql_geometry
    new Known.Method(Known.$sql_bool, "IIF", Known.$sql_geometry, Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.If(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_date toInt
    new Known.Method(Known.$sql_date, "toInt") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.conversion.DateToInt(nodeId, child);
      }
    },
    // sql_date minute
    new Known.Method(Known.$sql_date, "minute") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.ExtractMinute(nodeId, child);
      }
    },
    // sql_date hour
    new Known.Method(Known.$sql_date, "hour") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.ExtractHour(nodeId, child);
      }
    },
    // sql_date day
    new Known.Method(Known.$sql_date, "day") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.ExtractDay(nodeId, child);
      }
    },
    // sql_date month
    new Known.Method(Known.$sql_date, "month") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.ExtractMonth(nodeId, child);
      }
    },
    // sql_date year
    new Known.Method(Known.$sql_date, "year") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.ExtractYear(nodeId, child);
      }
    },
    // sql_date truncDay
    new Known.Method(Known.$sql_date, "truncDay") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.TruncDay(nodeId, child);
      }
    },
    // sql_date truncHour
    new Known.Method(Known.$sql_date, "truncHour") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.TruncHour(nodeId, child);
      }
    },
    // sql_date truncMinute
    new Known.Method(Known.$sql_date, "truncMinute") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.TruncMinute(nodeId, child);
      }
    },
    // sql_date truncSecond
    new Known.Method(Known.$sql_date, "truncSecond") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.TruncSecond(nodeId, child);
      }
    },
    // sql_date addDay sql_int
    new Known.Method(Known.$sql_date, "addDay", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.AddDay(nodeId, child, arguments.next());
      }
    },
    // sql_date addHour sql_int
    new Known.Method(Known.$sql_date, "addHour", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.AddHour(nodeId, child, arguments.next());
      }
    },
    // sql_date addMinute sql_int
    new Known.Method(Known.$sql_date, "addMinute", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.AddMinute(nodeId, child, arguments.next());
      }
    },
    // sql_date addSecond sql_int
    new Known.Method(Known.$sql_date, "addSecond", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.date.AddSecond(nodeId, child, arguments.next());
      }
    },
    // sql_datespan days
    new Known.Method(Known.$sql_datespan, "days") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.Days(nodeId, child);
      }
    },
    // sql_datespan hours
    new Known.Method(Known.$sql_datespan, "hours") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.Hours(nodeId, child);
      }
    },
    // sql_datespan minutes
    new Known.Method(Known.$sql_datespan, "minutes") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.Minutes(nodeId, child);
      }
    },
    // sql_datespan seconds
    new Known.Method(Known.$sql_datespan, "seconds") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.Seconds(nodeId, child);
      }
    },
    // sql_datespan totalHours
    new Known.Method(Known.$sql_datespan, "totalHours") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.TotalHours(nodeId, child);
      }
    },
    // sql_datespan totalMinutes
    new Known.Method(Known.$sql_datespan, "totalMinutes") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.TotalMinutes(nodeId, child);
      }
    },
    // sql_datespan totalSeconds
    new Known.Method(Known.$sql_datespan, "totalSeconds") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.TotalSeconds(nodeId, child);
      }
    },
    // sql_datespan truncHour
    new Known.Method(Known.$sql_datespan, "truncHour") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.TruncHour(nodeId, child);
      }
    },
    // sql_datespan truncMinute
    new Known.Method(Known.$sql_datespan, "truncMinute") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.TruncMinute(nodeId, child);
      }
    },
    // sql_datespan truncSecond
    new Known.Method(Known.$sql_datespan, "truncSecond") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.TruncSecond(nodeId, child);
      }
    },
    // sql_datespan addDay sql_int
    new Known.Method(Known.$sql_datespan, "addDay", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.AddDay(nodeId, child, arguments.next());
      }
    },
    // sql_datespan addHour sql_int
    new Known.Method(Known.$sql_datespan, "addHour", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.AddHour(nodeId, child, arguments.next());
      }
    },
    // sql_datespan addMinute sql_int
    new Known.Method(Known.$sql_datespan, "addMinute", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.AddMinute(nodeId, child, arguments.next());
      }
    },
    // sql_datespan addSecond sql_int
    new Known.Method(Known.$sql_datespan, "addSecond", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.datespan.AddSecond(nodeId, child, arguments.next());
      }
    },
    // sql_decimal sign
    new Known.Method(Known.$sql_decimal, "sign") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Sign(nodeId, child);
      }
    },
    // sql_decimal abs
    new Known.Method(Known.$sql_decimal, "abs") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Abs(nodeId, child);
      }
    },
    // sql_decimal ceil
    new Known.Method(Known.$sql_decimal, "ceil") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Ceil(nodeId, child);
      }
    },
    // sql_decimal floor
    new Known.Method(Known.$sql_decimal, "floor") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Floor(nodeId, child);
      }
    },
    // sql_decimal exp
    new Known.Method(Known.$sql_decimal, "exp") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Exp(nodeId, child);
      }
    },
    // sql_decimal ln
    new Known.Method(Known.$sql_decimal, "ln") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Ln(nodeId, child);
      }
    },
    // sql_decimal power sql_int
    new Known.Method(Known.$sql_decimal, "power", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Power(nodeId, child, arguments.next());
      }
    },
    // sql_decimal round sql_int
    new Known.Method(Known.$sql_decimal, "round", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Round(nodeId, child, arguments.next());
      }
    },
    // sql_decimal truncate sql_int
    new Known.Method(Known.$sql_decimal, "truncate", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Truncate(nodeId, child, arguments.next());
      }
    },
    // sql_geometry collectionSize
    new Known.Method(Known.$sql_geometry, "collectionSize") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.CollectionSize(nodeId, child);
      }
    },
    // sql_geometry x
    new Known.Method(Known.$sql_geometry, "x") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.X(nodeId, child);
      }
    },
    // sql_geometry xMax
    new Known.Method(Known.$sql_geometry, "xMax") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.XMax(nodeId, child);
      }
    },
    // sql_geometry xMin
    new Known.Method(Known.$sql_geometry, "xMin") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.XMin(nodeId, child);
      }
    },
    // sql_geometry y
    new Known.Method(Known.$sql_geometry, "y") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Y(nodeId, child);
      }
    },
    // sql_geometry yMax
    new Known.Method(Known.$sql_geometry, "yMax") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.YMax(nodeId, child);
      }
    },
    // sql_geometry yMin
    new Known.Method(Known.$sql_geometry, "yMin") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.YMin(nodeId, child);
      }
    },
    // sql_geometry length
    new Known.Method(Known.$sql_geometry, "length") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Length(nodeId, child);
      }
    },
    // sql_geometry element sql_int
    new Known.Method(Known.$sql_geometry, "element", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Element(nodeId, child, arguments.next());
      }
    },
    // sql_geometry split sql_geometry
    new Known.Method(Known.$sql_geometry, "split", Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Split(nodeId, child, arguments.next());
      }
    },
    // sql_geometry extract sql_int
    new Known.Method(Known.$sql_geometry, "extract", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Extract(nodeId, child, arguments.next());
      }
    },
    // sql_geometry strType
    new Known.Method(Known.$sql_geometry, "strType") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.GeometryType(nodeId, child);
      }
    },
    // sql_geometry intersects sql_geometry
    new Known.Method(Known.$sql_geometry, "intersects", Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Intersects(nodeId, child, arguments.next());
      }
    },
    // sql_geometry intersect sql_geometry
    new Known.Method(Known.$sql_geometry, "intersect", Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Intersection(nodeId, child, arguments.next());
      }
    },
    // sql_geometry distance sql_geometry
    new Known.Method(Known.$sql_geometry, "distance", Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.expressions.Distance(nodeId, child, arguments.next());
      }
    },
    // sql_geometry startPoint
    new Known.Method(Known.$sql_geometry, "startPoint") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.StartPoint(nodeId, child);
      }
    },
    // sql_geometry endPoint
    new Known.Method(Known.$sql_geometry, "endPoint") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.EndPoint(nodeId, child);
      }
    },
    // sql_geometry union
    new Known.Method(Known.$sql_geometry, "union") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Union(nodeId, child);
      }
    },
    // sql_geometry centroid
    new Known.Method(Known.$sql_geometry, "centroid") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Centroid(nodeId, child);
      }
    },
    // sql_geometry envelope
    new Known.Method(Known.$sql_geometry, "envelope") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Envelope(nodeId, child);
      }
    },
    // sql_geometry homogenize
    new Known.Method(Known.$sql_geometry, "homogenize") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Homogenize(nodeId, child);
      }
    },
    // sql_geometry interpolatePoint sql_decimal
    new Known.Method(Known.$sql_geometry, "interpolatePoint", Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.InterpolatePoint(nodeId, child, arguments.next());
      }
    },
    // sql_geometry buffer sql_decimal
    new Known.Method(Known.$sql_geometry, "buffer", Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Buffer(nodeId, child, arguments.next());
      }
    },
    // sql_geometry isEmpty
    new Known.Method(Known.$sql_geometry, "isEmpty") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.IsEmpty(nodeId, child);
      }
    },
    // sql_geometry isValid
    new Known.Method(Known.$sql_geometry, "isValid") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.IsValid(nodeId, child);
      }
    },
    // sql_geometry makeValid
    new Known.Method(Known.$sql_geometry, "makeValid") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.MakeValid(nodeId, child);
      }
    },
    // sql_geometry asGeoJSON
    new Known.Method(Known.$sql_geometry, "asGeoJSON") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.AsGeoJson(nodeId, child);
      }
    },
    // sql_geometry point sql_decimal sql_decimal sql_int
    new Known.Method(Known.$sql_geometry, "point", Known.$sql_decimal, Known.$sql_decimal, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Point(nodeId, arguments.next(), arguments.next(), arguments.next());
      }
    },
    // sql_int sign
    new Known.Method(Known.$sql_int, "sign") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Sign(nodeId, child);
      }
    },
    // sql_int abs
    new Known.Method(Known.$sql_int, "abs") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Abs(nodeId, child);
      }
    },
    // sql_int power sql_int
    new Known.Method(Known.$sql_int, "power", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Power(nodeId, child, arguments.next());
      }
    },
    // sql_string isInt
    new Known.Method(Known.$sql_string, "isInt") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.conversion.IsIntString(nodeId, child);
      }
    },
    // sql_string isNumeric
    new Known.Method(Known.$sql_string, "isNumeric") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.conversion.IsNumericString(nodeId, child);
      }
    },
    // sql_string toInt
    new Known.Method(Known.$sql_string, "toInt") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.conversion.StringToInt(nodeId, child);
      }
    },
    // sql_string toDecimal
    new Known.Method(Known.$sql_string, "toDecimal") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.conversion.ToDecimal(nodeId, child);
      }
    },
    // sql_string toDate
    new Known.Method(Known.$sql_string, "toDate") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.conversion.ToDate(nodeId, child);
      }
    },
    // sql_string isEmpty
    new Known.Method(Known.$sql_string, "isEmpty") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.IsEmpty(nodeId, child);
      }
    },
    // sql_string length
    new Known.Method(Known.$sql_string, "length") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.Length(nodeId, child);
      }
    },
    // sql_string indexOf sql_string
    new Known.Method(Known.$sql_string, "indexOf", Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.IndexOf(nodeId, arguments.next(), child, null);
      }
    },
    // sql_string indexOf sql_string sql_int
    new Known.Method(Known.$sql_string, "indexOf", Known.$sql_string, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.IndexOf(nodeId, arguments.next(), child, arguments.next());
      }
    },
    // sql_string contains sql_string
    new Known.Method(Known.$sql_string, "contains", Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, new org.zenframework.z8.blls.evalsql.functions.string.IndexOf(nodeId, arguments.next(), child, new SqlConst.Integer(nodeId, 0)), Operation.NotEq, new SqlConst.Integer(nodeId, -1));
      }
    },
    // sql_string contains sql_string sql_int
    new Known.Method(Known.$sql_string, "contains", Known.$sql_string, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, new org.zenframework.z8.blls.evalsql.functions.string.IndexOf(nodeId, arguments.next(), child, arguments.next()), Operation.NotEq, new SqlConst.Integer(nodeId, -1));
      }
    },
    // sql_string patIndexOf sql_string
    new Known.Method(Known.$sql_string, "patIndexOf", Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.RegIndexOf(nodeId, child, arguments.next());
      }
    },
    // sql_string like sql_string
    new Known.Method(Known.$sql_string, "like", Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.Like(nodeId, child, arguments.next());
      }
    },
    // sql_string like sql_string sql_string
    new Known.Method(Known.$sql_string, "like", Known.$sql_string, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.Like(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_string regLike sql_string
    new Known.Method(Known.$sql_string, "regLike", Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.RegLike(nodeId, child, arguments.next());
      }
    },
    // sql_string toLower
    new Known.Method(Known.$sql_string, "toLower") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.Lower(nodeId, child);
      }
    },
    // sql_string toUpper
    new Known.Method(Known.$sql_string, "toUpper") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.Upper(nodeId, child);
      }
    },
    // sql_string trimLeft
    new Known.Method(Known.$sql_string, "trimLeft") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.LTrim(nodeId, child);
      }
    },
    // sql_string trimRight
    new Known.Method(Known.$sql_string, "trimRight") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.RTrim(nodeId, child);
      }
    },
    // sql_string trimAll
    new Known.Method(Known.$sql_string, "trimAll") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.RTrim(nodeId, new org.zenframework.z8.blls.evalsql.functions.string.LTrim(nodeId, child));
      }
    },
    // sql_string padLeft sql_int
    new Known.Method(Known.$sql_string, "padLeft", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.LPad(nodeId, child, arguments.next(), null);
      }
    },
    // sql_string padLeft sql_int sql_string
    new Known.Method(Known.$sql_string, "padLeft", Known.$sql_int, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.LPad(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_string padRight sql_int
    new Known.Method(Known.$sql_string, "padRight", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.RPad(nodeId, child, arguments.next(), null);
      }
    },
    // sql_string padRight sql_int sql_string
    new Known.Method(Known.$sql_string, "padRight", Known.$sql_int, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.RPad(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_string replace sql_string sql_string
    new Known.Method(Known.$sql_string, "replace", Known.$sql_string, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.Replace(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_string replaceAll sql_string sql_string
    new Known.Method(Known.$sql_string, "replaceAll", Known.$sql_string, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.RegReplace(nodeId, child, arguments.next(), arguments.next(), null);
      }
    },
    // sql_string replaceAll sql_string sql_string sql_string
    new Known.Method(Known.$sql_string, "replaceAll", Known.$sql_string, Known.$sql_string, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.RegReplace(nodeId, child, arguments.next(), arguments.next(), arguments.next());
      }
    },
    // sql_string substring sql_int sql_int
    new Known.Method(Known.$sql_string, "substring", Known.$sql_int, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.Substr(nodeId, child, arguments.next(), arguments.next());
      }
    },
    // sql_string reverse
    new Known.Method(Known.$sql_string, "reverse") {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.Reverse(nodeId, child);
      }
    },
    // sql_string json sql_string
    new Known.Method(Known.$sql_string, "json", Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.GetJson(nodeId, child, arguments.next());
      }
    },
    // sql_string json sql_int
    new Known.Method(Known.$sql_string, "json", Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments) {
        return new org.zenframework.z8.blls.evalsql.functions.string.GetJson(nodeId, child, arguments.next());
      }
    }
  );
  static final List<Known> KNOWN_PRIORITIES = List.of(
    Known.$sql_bool,
    Known.$sql_date,
    Known.$sql_datespan,
    Known.$sql_decimal,
    Known.$sql_geometry,
    Known.$sql_guid,
    Known.$sql_int,
    Known.$sql_string
  );
  static final List<Known.Cast> KNOWN_CASTS = List.of(
    new Known.Cast(Known.$bool, Known.$sql_bool),
    new Known.Cast(Known.$date, Known.$sql_date),
    new Known.Cast(Known.$datespan, Known.$sql_datespan),
    new Known.Cast(Known.$decimal, Known.$sql_decimal),
    new Known.Cast(Known.$guid, Known.$sql_guid),
    new Known.Cast(Known.$int, Known.$sql_int),
    new Known.Cast(Known.$string, Known.$sql_string),
    new Known.Cast(Known.$BinaryField, Known.$sql_binary),
    new Known.Cast(Known.$BoolField, Known.$sql_bool),
    new Known.Cast(Known.$DatespanField, Known.$sql_datespan),
    new Known.Cast(Known.$DatetimeField, Known.$sql_date),
    new Known.Cast(Known.$DecimalField, Known.$sql_decimal),
    new Known.Cast(Known.$GeometryField, Known.$sql_geometry),
    new Known.Cast(Known.$GuidField, Known.$sql_guid),
    new Known.Cast(Known.$IntField, Known.$sql_int),
    new Known.Cast(Known.$StringField, Known.$sql_string)
  );
  static final Known.Cast KNOWN_CAST_$sql_int$sql_decimal = new Known.Cast(Known.$sql_int, Known.$sql_decimal);
  static final List<Known.OperatorUnary> KNOWN_OPERATORS_UNARY = List.of(
    new Known.OperatorUnary(Known.$sql_bool, org.zenframework.z8.blls.ast.Operation.NOT),
    new Known.OperatorUnary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.ADD),
    new Known.OperatorUnary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.SUBTRACT),
    new Known.OperatorUnary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.ADD),
    new Known.OperatorUnary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.SUBTRACT)
  );
  static final List<Known.OperatorBinary> KNOWN_OPERATORS_BINARY = List.of(
    // sql_bool EQUAL sql_bool
    new Known.OperatorBinary(Known.$sql_bool, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_bool) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_bool NOT_EQUAL sql_bool
    new Known.OperatorBinary(Known.$sql_bool, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_bool) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_bool AND sql_bool
    new Known.OperatorBinary(Known.$sql_bool, org.zenframework.z8.blls.ast.Operation.AND, Known.$sql_bool) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.And(nodeId, left, right);
      }
    },
    // sql_bool OR sql_bool
    new Known.OperatorBinary(Known.$sql_bool, org.zenframework.z8.blls.ast.Operation.OR, Known.$sql_bool) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Or(nodeId, left, right);
      }
    },
    // sql_date ADD sql_datespan
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.ADD, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Add, right);
      }
    },
    // sql_date SUBTRACT sql_datespan
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.SUBTRACT, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Sub, right);
      }
    },
    // sql_date SUBTRACT sql_date
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.SUBTRACT, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Sub, right);
      }
    },
    // sql_date LESS sql_date
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.LESS, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LT, right);
      }
    },
    // sql_date MORE_EQUAL sql_date
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.MORE_EQUAL, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GT, right);
      }
    },
    // sql_date LESS_EQUAL sql_date
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.LESS_EQUAL, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LE, right);
      }
    },
    // sql_date MORE_EQUAL sql_date
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.MORE_EQUAL, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GE, right);
      }
    },
    // sql_date EQUAL sql_date
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_date NOT_EQUAL sql_date
    new Known.OperatorBinary(Known.$sql_date, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_datespan ADD sql_date
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.ADD, Known.$sql_date) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Add, right);
      }
    },
    // sql_datespan ADD sql_datespan
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.ADD, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Add, right);
      }
    },
    // sql_datespan SUBTRACT sql_datespan
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.SUBTRACT, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Sub, right);
      }
    },
    // sql_datespan LESS sql_datespan
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.LESS, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LT, right);
      }
    },
    // sql_datespan MORE sql_datespan
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.MORE, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GT, right);
      }
    },
    // sql_datespan LESS_EQUAL sql_datespan
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.LESS_EQUAL, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LE, right);
      }
    },
    // sql_datespan MORE_EQUAL sql_datespan
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.MORE_EQUAL, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GE, right);
      }
    },
    // sql_datespan EQUAL sql_datespan
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_datespan NOT_EQUAL sql_datespan
    new Known.OperatorBinary(Known.$sql_datespan, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_datespan) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_decimal ADD sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.ADD, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Add, right);
      }
    },
    // sql_decimal SUBTRACT sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.SUBTRACT, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Sub, right);
      }
    },
    // sql_decimal MULTIPLY sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.MULTIPLY, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Mul, right);
      }
    },
    // sql_decimal DIVIDE sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.DIVIDE, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Div, right);
      }
    },
    // sql_decimal MODULE sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.MODULE, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Mod, right);
      }
    },
    // sql_decimal ADD sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.ADD, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Add, right);
      }
    },
    // sql_decimal SUBTRACT sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.SUBTRACT, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Sub, right);
      }
    },
    // sql_decimal MULTIPLY sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.MULTIPLY, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Mul, right);
      }
    },
    // sql_decimal DIVIDE sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.DIVIDE, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Div, right);
      }
    },
    // sql_decimal MODULE sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.MODULE, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Mod, right);
      }
    },
    // sql_decimal LESS sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.LESS, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LT, right);
      }
    },
    // sql_decimal MORE sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.MORE, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GT, right);
      }
    },
    // sql_decimal LESS_EQUAL sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.LESS_EQUAL, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LE, right);
      }
    },
    // sql_decimal MORE_EQUAL sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.MORE_EQUAL, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GE, right);
      }
    },
    // sql_decimal EQUAL sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_decimal NOT_EQUAL sql_int
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_decimal LESS sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.LESS, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LT, right);
      }
    },
    // sql_decimal MORE sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.MORE, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GT, right);
      }
    },
    // sql_decimal LESS_EQUAL sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.LESS_EQUAL, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LE, right);
      }
    },
    // sql_decimal MORE_EQUAL sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.MORE_EQUAL, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GE, right);
      }
    },
    // sql_decimal EQUAL sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_decimal NOT_EQUAL sql_decimal
    new Known.OperatorBinary(Known.$sql_decimal, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_geometry EQUAL sql_geometry
    new Known.OperatorBinary(Known.$sql_geometry, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_geometry NOT_EQUAL sql_geometry
    new Known.OperatorBinary(Known.$sql_geometry, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_geometry AND sql_geometry
    new Known.OperatorBinary(Known.$sql_geometry, org.zenframework.z8.blls.ast.Operation.AND, Known.$sql_geometry) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.functions.geometry.Intersects(nodeId, left, right);
      }
    },
    // sql_guid EQUAL sql_guid
    new Known.OperatorBinary(Known.$sql_guid, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_guid) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_guid NOT_EQUAL sql_guid
    new Known.OperatorBinary(Known.$sql_guid, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_guid) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_int ADD sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.ADD, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Add, right);
      }
    },
    // sql_int SUBTRACT sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.SUBTRACT, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Sub, right);
      }
    },
    // sql_int MULTIPLY sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.MULTIPLY, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Mul, right);
      }
    },
    // sql_int DIVIDE sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.DIVIDE, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.functions.numeric.Floor(nodeId, new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Div, right));
      }
    },
    // sql_int MODULE sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.MODULE, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Mod, right);
      }
    },
    // sql_int ADD sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.ADD, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Add, right);
      }
    },
    // sql_int SUBTRACT sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.SUBTRACT, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Sub, right);
      }
    },
    // sql_int MULTIPLY sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.MULTIPLY, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Mul, right);
      }
    },
    // sql_int DIVIDE sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.DIVIDE, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Div, right);
      }
    },
    // sql_int MODULE sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.MODULE, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Mul(nodeId, left, Operation.Mod, right);
      }
    },
    // sql_int LESS sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.LESS, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LT, right);
      }
    },
    // sql_int MORE sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.MORE, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GT, right);
      }
    },
    // sql_int LESS_EQUAL sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.LESS_EQUAL, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LE, right);
      }
    },
    // sql_int MORE_EQUAL sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.MORE_EQUAL, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GE, right);
      }
    },
    // sql_int EQUAL sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_int NOT_EQUAL sql_int
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_int) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_int LESS sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.LESS, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LT, right);
      }
    },
    // sql_int MORE sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.MORE, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GT, right);
      }
    },
    // sql_int LESS_EQUAL sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.LESS_EQUAL, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LE, right);
      }
    },
    // sql_int MORE_EQUAL sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.MORE_EQUAL, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GE, right);
      }
    },
    // sql_int EQUAL sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_int NOT_EQUAL sql_decimal
    new Known.OperatorBinary(Known.$sql_int, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_decimal) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    },
    // sql_string ADD sql_string
    new Known.OperatorBinary(Known.$sql_string, org.zenframework.z8.blls.ast.Operation.ADD, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Add(nodeId, left, Operation.Add, right);
      }
    },
    // sql_string LESS sql_string
    new Known.OperatorBinary(Known.$sql_string, org.zenframework.z8.blls.ast.Operation.LESS, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LT, right);
      }
    },
    // sql_string MORE sql_string
    new Known.OperatorBinary(Known.$sql_string, org.zenframework.z8.blls.ast.Operation.MORE, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GT, right);
      }
    },
    // sql_string LESS_EQUAL sql_string
    new Known.OperatorBinary(Known.$sql_string, org.zenframework.z8.blls.ast.Operation.LESS_EQUAL, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.LE, right);
      }
    },
    // sql_string MORE_EQUAL sql_string
    new Known.OperatorBinary(Known.$sql_string, org.zenframework.z8.blls.ast.Operation.MORE_EQUAL, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.GE, right);
      }
    },
    // sql_string EQUAL sql_string
    new Known.OperatorBinary(Known.$sql_string, org.zenframework.z8.blls.ast.Operation.EQUAL, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.Eq, right);
      }
    },
    // sql_string NOT_EQUAL sql_string
    new Known.OperatorBinary(Known.$sql_string, org.zenframework.z8.blls.ast.Operation.NOT_EQUAL, Known.$sql_string) {
      @Override
      protected SqlToken run(int nodeId, SqlToken left, SqlToken right) {
        return new org.zenframework.z8.blls.evalsql.expressions.Rel(nodeId, left, Operation.NotEq, right);
      }
    }
  );

  final int[] knownTypesResolved;
  final MapInt<Known> knownFields;
  final MapInt<Known.Method> knownMethods;
  final MapInt<Known> knownPriorities;
  final MapInt<Known.Cast> knownCasts;
  final MapInt<Known.OperatorUnary> knownOperatorsUnary;
  final MapInt<Known.OperatorBinary> knownOperatorsBinary;
  int $sql_int$sql_decimal;

  public Ast(org.zenframework.z8.blls.ast.Ast ast) throws Exception {
    super(ast);

    knownTypesResolved = stream(Known.values())
      .mapToInt(known ->
        Resolver.runSimple(this, known.qualifiedName().iterator())
          .filter(it -> it.data() instanceof org.zenframework.z8.blls.ast.Declaration)
          .orElseThrow(Exception::new)
          .id()
      )
      .toArray();
    knownFields = KNOWN_FIELDS.stream().collect(collectorMapInt(it -> knownOf(it).id()));
    knownMethods = KNOWN_METHODS.stream().collect(collectorMapInt(it -> {
      var key = new Extra.DeclarationMethod.Key(
        it.name,
        it.arguments.stream()
          .<Expression.Type>map(it2 -> new Expression.Type.Declaration(knownOf(it2).id()))
          .toList()
      );
      return knownOf(it.declaration).data()
        .children(this)
        .gather(only(org.zenframework.z8.blls.ast.Declaration.Method.class))
        .filter(it2 -> key.equals(Extra.DeclarationMethod.Key.of(analyzer(), it2).orElse(null)))
        .findFirst()
        .orElseThrow(Exception::new)
        .id();
    }));
    knownPriorities = KNOWN_PRIORITIES.stream().collect(collectorMapInt(it ->
      knownOf(it).data()
        .children(this)
        .gather(only(org.zenframework.z8.blls.ast.Declaration.Priority.class))
        .findFirst()
        .orElseThrow(Exception::new)
        .id()
    ));
    knownCasts = KNOWN_CASTS.stream().collect(collectorMapInt(this::resolveCast));
    $sql_int$sql_decimal = resolveCast(KNOWN_CAST_$sql_int$sql_decimal);
    knownOperatorsUnary = KNOWN_OPERATORS_UNARY.stream().collect(collectorMapInt(it -> {
      var key = new Extra.DeclarationOperator.Key(it.operation(), null);
      return knownOf(it.declaration()).data()
        .children(this)
        .gather(only(org.zenframework.z8.blls.ast.Declaration.Operator.class))
        .filter(it2 -> key.equals(Extra.DeclarationOperator.Key.of(analyzer(), it2).orElse(null)))
        .findFirst()
        .orElseThrow(Exception::new)
        .id();
    }));
    knownOperatorsBinary = KNOWN_OPERATORS_BINARY.stream().collect(collectorMapInt(it -> {
      var key = new Extra.DeclarationOperator.Key(it.operation, new Expression.Type.Declaration(knownOf(it.right).id()));
      return knownOf(it.declaration).data()
        .children(this)
        .gather(only(org.zenframework.z8.blls.ast.Declaration.Operator.class))
        .filter(it2 -> key.equals(Extra.DeclarationOperator.Key.of(analyzer(), it2).orElse(null)))
        .findFirst()
        .orElseThrow(Exception::new)
        .id();
    }));
  }

  public org.zenframework.z8.blls.analyzer.Ast analyzer() {
    return new org.zenframework.z8.blls.analyzer.Ast(this);
  }

  Node<org.zenframework.z8.blls.ast.Declaration> knownOf(Known known) {
    return node(knownTypesResolved[known.ordinal()]);
  }

  private <T> Collector<T, ?, MapInt<T>> collectorMapInt(ToIntFunction<T> keyOf) {
    return Collector.of(
      MapInt<T>::new,
      (downstream, it) -> downstream.put(keyOf.applyAsInt(it), it),
      (a, b) -> {
        a.putAll(b);
        return a;
      }
    );
  }

  private int resolveCast(Known.Cast it) {
    var typeTarget = new Expression.Type.Declaration(knownOf(it.target()).id());
    return knownOf(it.declaration()).data()
      .children(this)
      .gather(only(org.zenframework.z8.blls.ast.Declaration.Cast.class))
      .filter(it2 -> typeTarget.equals(analyzer().typeOf(it2.data().definition(this)).orElse(null)))
      .findFirst()
      .orElseThrow(Exception::new)
      .id();
  }

  enum Known {
    $bool,
    $date,
    $datespan,
    $decimal,
    $guid,
    $int,
    $string,
    $sql_binary,
    $sql_bool,
    $sql_date,
    $sql_datespan,
    $sql_decimal,
    $sql_geometry,
    $sql_guid,
    $sql_int,
    $sql_string,
    $Expression,
    $BinaryField,
    $BoolField,
    $DatespanField,
    $DatetimeField,
    $DecimalField,
    $GeometryField,
    $GuidField,
    $IntField,
    $StringField,
    $Sql;

    List<String> qualifiedName() {
      return switch (this) {
        case $bool,
             $date,
             $datespan,
             $decimal,
             $guid,
             $int,
             $string -> List.of("org", "zenframework", "z8", "lang", this.name().substring(1));
        case $sql_binary,
             $sql_bool,
             $sql_date,
             $sql_datespan,
             $sql_decimal,
             $sql_geometry,
             $sql_guid,
             $sql_int,
             $sql_string -> List.of("org", "zenframework", "z8", "lang", "sql", this.name().substring(1));
        case $Expression,
             $BinaryField,
             $BoolField,
             $DatespanField,
             $DatetimeField,
             $DecimalField,
             $GeometryField,
             $GuidField,
             $IntField,
             $StringField -> List.of("org", "zenframework", "z8", "base", "table", "value", this.name().substring(1));
        case $Sql -> List.of("org", "zenframework", "z8", "base", "table", "Sql");
      };
    }

    FieldType type() {
      return switch (this) {
        case $bool, $sql_bool, $BoolField -> FieldType.Boolean;
        case $date, $sql_date -> FieldType.Date;
        case $datespan, $sql_datespan, $DatespanField -> FieldType.Datespan;
        case $decimal, $sql_decimal, $DecimalField -> FieldType.Decimal;
        case $guid, $sql_guid, $GuidField -> FieldType.Guid;
        case $int, $sql_int, $IntField -> FieldType.Integer;
        case $string, $sql_string, $StringField -> FieldType.String;
        case $sql_binary, $BinaryField -> FieldType.Binary;
        case $sql_geometry, $GeometryField -> FieldType.Geometry;
        case $DatetimeField -> FieldType.Datetime;
        case $Expression, $Sql -> throw new AssertionError();
      };
    }

    abstract static class Method {
      final Known declaration;
      final String name;
      final List<Known> arguments;

      Method(Known declaration, String name, Known... arguments) {
        this.declaration = declaration;
        this.name = name;
        this.arguments = List.of(arguments);
      }

      abstract SqlToken run(int nodeId, SqlToken child, Iterator<SqlToken> arguments);

      abstract static class Static extends Method {
        Static(Known declaration, String name, Known... arguments) {
          super(declaration, name, arguments);
        }

        @Override
        abstract SqlToken run(int nodeId, @Nullable SqlToken child, Iterator<SqlToken> arguments);
      }
    }

    record Cast(Known declaration, Known target) {
    }

    record OperatorUnary(Known declaration, org.zenframework.z8.blls.ast.Operation operation) {
    }

    abstract static class OperatorBinary {
      final Known declaration;
      final org.zenframework.z8.blls.ast.Operation operation;
      final Known right;

      OperatorBinary(Known declaration, org.zenframework.z8.blls.ast.Operation operation, Known right) {
        this.declaration = declaration;
        this.operation = operation;
        this.right = right;
      }

      abstract SqlToken run(int nodeId, SqlToken left, SqlToken right);
    }
  }

  public static final class Exception extends java.lang.RuntimeException {
  }
}
