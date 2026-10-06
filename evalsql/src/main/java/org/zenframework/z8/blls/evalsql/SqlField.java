package org.zenframework.z8.blls.evalsql;

import org.zenframework.z8.blls.ast.Annotation;
import org.zenframework.z8.blls.ast.Declaration;
import org.zenframework.z8.blls.ast.Node;

public class SqlField extends SqlToken {
  private final FieldType type;
  private final String ownerName;
  private final String fieldName;

  public SqlField(int nodeId, FieldType type, Ast ast, Node<Declaration.Field> field) {
    super(nodeId);
    this.type = type;

    var klass = ast.analyzer()
      .inheritanceKlasses(ast.analyzer().declarationOf(field))
      .findFirst()
      .orElseThrow();
    ownerName = ast.analyzer()
      .annotationOf(klass, Annotation.NAME)
      .flatMap(ast.analyzer()::annotationConstant)
      .orElse(null)
      instanceof String v
      ? v
      : klass.data().name(ast);
    fieldName = field.data()
      .annotations(ast)
      .filter(it -> it.data().name(ast).equals(Annotation.NAME))
      .findFirst()
      .flatMap(ast.analyzer()::annotationConstant)
      .orElse(null)
      instanceof String v
      ? v
      : field.data().name(ast);
  }

  private static String quote(DatabaseVendor vendor, String value) {
    char start = vendor == DatabaseVendor.SqlServer ? '[' : '"';
    char end = vendor == DatabaseVendor.SqlServer ? ']' : '"';
    return start + value + end;
  }

//  @Override
//  public void collectFields(Collection<IField> fields) {
//    if (field.isExpression()) {
//      Expression expression = (Expression) field;
//      SqlToken token = expression.expression();
//
//      if (token != null)
//        token.collectFields(fields);
//    } else
//      fields.add(field);
//  }

  @Override
  public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
//    Aggregation aggregation = field.getAggregation();
//
//    if (aggregation != Aggregation.None)
//      options.disableAggregation();

    SqlToken token = getToken(vendor, options, logicalContext);

//    if (aggregation != Aggregation.None)
//      options.enableAggregation();

//    return aggregate(token, options.isAggregationEnabled() ? aggregation : Aggregation.None).format(vendor, options, logicalContext);
    return token.format(vendor, options, logicalContext);
  }

  private SqlToken getToken(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) {
    String alias = "%s.%s".formatted(ownerName, quote(vendor, fieldName));

    // and field like '' /*setExpression(field.like(''))*/
    // and field like '' /*setExpression(field.like('').IIF(1, 0) == 1)*/
    if (logicalContext && type() == FieldType.Boolean/* && !(field instanceof Expression)*/)
      alias += "=1";

    return new SqlStringToken(nodeId(), alias, type());
  }

//  public SqlToken aggregate(SqlToken token, Aggregation aggregation) {
//    switch (aggregation) {
//      case Sum:
//        return new Sum(token);
//      case Max:
//        return new Max(token);
//      case Min:
//        return new Min(token);
//      case Average:
//        return new Average(token);
//      case Count:
//        return new Count(token);
//      case Array:
//        return new Array(token, false);
//      case Distinct:
//        return new Array(token, true);
//      case Concat:
//        return new Concat(token);
//      default:
//        return token;
//    }
//  }

  @Override
  public FieldType type() {
    return type;
  }

//  public Aggregation aggregation() {
//    return field.aggregation;
//  }
}
