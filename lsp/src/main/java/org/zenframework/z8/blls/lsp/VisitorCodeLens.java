package org.zenframework.z8.blls.lsp;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.analyzer.Caster;
import org.zenframework.z8.blls.ast.Declaration;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Node;
import org.zenframework.z8.blls.evalsql.*;

import static org.zenframework.z8.blls.analyzer.Ast.only;

abstract class VisitorCodeLens extends Visitor {
  protected abstract Ast astLsp();

  protected abstract DatabaseVendor vendor();

  protected abstract void success(CodeLens value);

  protected abstract Expression.Type.Declaration typeOf(String name);

  @Override
  protected void declaration(Node<Declaration> node) {
    node.data()
      .children(ast())
      .gather(only(Declaration.Field.class))
      .forEach(it -> visit(it.id()));
    node.data()
      .children(ast())
      .gather(only(Declaration.Initialize.class))
      .forEach(it -> visit(it.id()));
  }

  @Override
  protected void declarationField(Node<Declaration.Field> node) {
    var $Field = typeOf("Field");
    var typeSource = ast().analyzer().typeOf(node.data().definition(ast())).orElse(null);
    var v = node.data()
      .value(ast())
      .map(it -> it.data().value(ast()))
      .orElse(null);

    if (typeSource != null && Caster.runSimple(ast().analyzer(), typeSource, $Field) && v != null) {
      var e = ast().analyzer().extra(node, Extra.DeclarationField.class);
      var token = visitEval(v, e.valueCastId).orElseGet(() -> new Uneval(v.id(), FieldType.None));

      try {
        String sql = token.format(vendor(), new FormatOptions());
        success(new CodeLens(
          astLsp().range(astLsp().declarationFieldName(node)),
          new Command(sql, ""),
          null
        ));
      } catch (SqlToken.UnknownDatabaseException | Uneval.Exception _) {
      }
    } else {
      super.declarationField(node);
    }
  }

  @Override
  protected void declarationInitialize(Node<Declaration.Initialize> node) {
    var $Field = typeOf("Field");
    var typeSource = ast().analyzer()
      .initializeAccesses(node.data())
      .findFirst()
      .flatMap(ast()::type)
      .orElse(null);

    if (
      typeSource != null &&
        Caster.runSimple(ast().analyzer(), typeSource, $Field) &&
        node.data().child(ast()).data().value(ast()) instanceof Node(int id, Expression.Binary v)
    ) {
      var e = ast().analyzer().extra(new Node<>(id, v), Extra.ExpressionBinary.class);
      var token = visitEval(v.right(ast()), e.rightCastId).orElseGet(() -> new Uneval(v.rightId(), FieldType.None));

      try {
        String sql = token.format(vendor(), new FormatOptions());
        success(new CodeLens(
          astLsp().anchor(node),
          new Command(sql, ""),
          null
        ));
      } catch (SqlToken.UnknownDatabaseException | Uneval.Exception _) {
      }
    } else {
      super.declarationInitialize(node);
    }
  }
}
