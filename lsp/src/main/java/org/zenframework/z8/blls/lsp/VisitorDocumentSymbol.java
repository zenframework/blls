package org.zenframework.z8.blls.lsp;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolTag;
import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.ast.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

abstract class VisitorDocumentSymbol extends Visitor {
  private @Nullable DocumentSymbol documentSymbol = null;
  private @Nullable DocumentSymbol documentSymbolOld = null;
  private boolean documentSymbolUsed = false;

  @Override
  protected abstract Ast ast();

  protected abstract void success(List<DocumentSymbol> value);

  private void setDocumentSymbol(DocumentSymbol value) {
    this.documentSymbolOld = value;
  }

  private DocumentSymbol visitDocumentSymbol(int id) {
    this.documentSymbolUsed = true;
    visit(id);
    assert documentSymbol != null;
    return documentSymbol;
  }

  @Override
  public void visit(int id) {
    boolean documentSymbolUsed = this.documentSymbolUsed;
    this.documentSymbolUsed = false;

    super.visit(id);

    this.documentSymbol = this.documentSymbolOld;
    this.documentSymbolOld = null;
    assert documentSymbolUsed == (this.documentSymbol != null);
  }

  @Override
  protected void source(Node<Source> node) {
    success(
      node.data()
        .children(ast())
        .map(it -> visitDocumentSymbol(it.id()))
        .toList()
    );
  }

  @Override
  protected void klass(Node<Klass> node) {
    var ds = new DocumentSymbol(
      node.data().name(ast()),
      SymbolKind.Class,
      ast().range(node),
      ast().range(ast().klassName(node)),
      "class " + node.data().name(ast()),
      node.data()
        .child(ast())
        .data()
        .children(ast())
        .flatMap(it ->
          it.data() instanceof Declaration.Initialize v
            ? declarationInitializeIfExpressionObject(new Node<>(it.id(), v)).stream()
            : Stream.of(visitDocumentSymbol(it.id()))
        )
        .toList()
    );
    node.data()
      .annotations(ast())
      .filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
      .findFirst()
      .ifPresent(_ -> ds.setTags(List.of(SymbolTag.Deprecated)));
    setDocumentSymbol(ds);
  }

  @Override
  protected void enumeration(Node<Enumeration> node) {
    var ds = new DocumentSymbol(
      node.data().name(ast()),
      SymbolKind.Enum,
      ast().range(node),
      ast().range(ast().enumerationName(node)),
      "enum " + node.data().name(ast()),
      node.data()
        .children(ast())
        .map(it -> visitDocumentSymbol(it.id()))
        .toList()
    );
    node.data()
      .annotations(ast())
      .filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
      .findFirst()
      .ifPresent(_ -> ds.setTags(List.of(SymbolTag.Deprecated)));
    setDocumentSymbol(ds);
  }

  @Override
  protected void declarationField(Node<Declaration.Field> node) {
    if (
      node.data()
        .value(ast())
        .map(it -> it.data().value(ast()))
        .orElse(null)
        instanceof Node(int id, Expression.Object v)
    ) {
      var ds = new DocumentSymbol(
        "object",
        SymbolKind.Object,
        ast().range(node),
        ast().range(ast().expressionObjectKeyword(new Node<>(id, v))),
        null,
        v.child(ast())
          .data()
          .children(ast())
          .flatMap(it ->
            it.data() instanceof Declaration.Initialize v2
              ? declarationInitializeIfExpressionObject(new Node<>(it.id(), v2)).stream()
              : Stream.of(visitDocumentSymbol(it.id()))
          )
          .toList()
      );
      node.data()
        .annotations(ast())
        .filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
        .findFirst()
        .ifPresent(_ -> ds.setTags(List.of(SymbolTag.Deprecated)));
      setDocumentSymbol(ds);
    } else {
      var ds = new DocumentSymbol(
        node.data().name(ast()),
        SymbolKind.Field,
        ast().range(node),
        ast().range(ast().declarationFieldName(node)),
        "%s %s::%s".formatted(
          ast().analyzer().typeOf(node.data().definition(ast())).map(ast()::toString).orElse("<error>"),
          ast().toString(new Expression.Type.Declaration(ast().analyzer().declarationOf(node).id())),
          node.data().name(ast())
        )
      );
      node.data()
        .annotations(ast())
        .filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
        .findFirst()
        .ifPresent(_ -> ds.setTags(List.of(SymbolTag.Deprecated)));
      setDocumentSymbol(ds);
    }
  }

  private Optional<DocumentSymbol> declarationInitializeIfExpressionObject(Node<Declaration.Initialize> node) {
    if (
      node.data().child(ast()).data().value(ast()).data() instanceof Expression.Binary v &&
        v.right(ast()).data() instanceof Expression.Object v2
    ) {
      return Optional.of(new DocumentSymbol(
        "object",
        SymbolKind.Object,
        ast().range(node),
        ast().range(ast().expressionObjectKeyword(new Node<>(v.rightId(), v2))),
        null,
        v2.child(ast())
          .data()
          .children(ast())
          .flatMap(it ->
            it.data() instanceof Declaration.Initialize v3
              ? declarationInitializeIfExpressionObject(new Node<>(it.id(), v3)).stream()
              : Stream.of(visitDocumentSymbol(it.id()))
          )
          .toList()
      ));
    } else return Optional.empty();
  }

  @Override
  protected void declarationMethod(Node<Declaration.Method> node) {
    var ds = new DocumentSymbol(
      node.data().name(ast()),
      SymbolKind.Method,
      ast().range(node),
      ast().range(ast().declarationMethodName(node)),
      "%s %s::%s(%s)".formatted(
        ast().analyzer().typeOf(node.data().definition(ast())).map(ast()::toString).orElse("<error>"),
        ast().toString(new Expression.Type.Declaration(ast().analyzer().declarationOf(node).id())),
        node.data().name(ast()),
        node.data().parameters(ast()).map(it -> "%s %s".formatted(
          ast().analyzer().typeOf(it.data().definition(ast())).map(ast()::toString).orElse("<error>"),
          it.data().name(ast())
        )).collect(joining(", "))
      )
    );
    node.data()
      .annotations(ast())
      .filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
      .findFirst()
      .ifPresent(_ -> ds.setTags(List.of(SymbolTag.Deprecated)));
    setDocumentSymbol(ds);
  }

  @Override
  protected void declarationPriority(Node<Declaration.Priority> node) {
    var ds = new DocumentSymbol(
      "priority",
      SymbolKind.Method,
      ast().range(node),
      ast().range(ast().declarationPriorityKeyword(node)),
      "%1$s %1$s::operatorPriority()".formatted(
        ast().toString(new Expression.Type.Declaration(ast().analyzer().declarationOf(node).id()))
      )
    );
    node.data()
      .annotations(ast())
      .filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
      .findFirst()
      .ifPresent(_ -> ds.setTags(List.of(SymbolTag.Deprecated)));
    setDocumentSymbol(ds);
  }

  @Override
  protected void declarationCast(Node<Declaration.Cast> node) {
    var ds = new DocumentSymbol(
      node.data().definition(ast()).data().name(ast()),
      SymbolKind.Method,
      ast().range(node),
      ast().range(ast().declarationCastKeyword(node)),
      "%s %s::%s()".formatted(
        ast().analyzer().typeOf(node.data().definition(ast())).map(ast()::toString).orElse("<error>"),
        ast().toString(new Expression.Type.Declaration(ast().analyzer().declarationOf(node).id())),
        node.data().definition(ast()).data().name(ast())
      )
    );
    node.data()
      .annotations(ast())
      .filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
      .findFirst()
      .ifPresent(_ -> ds.setTags(List.of(SymbolTag.Deprecated)));
    setDocumentSymbol(ds);
  }

  @Override
  protected void declarationOperator(Node<Declaration.Operator> node) {
    var ds = new DocumentSymbol(
      node.data().operation().toString(),
      SymbolKind.Method,
      ast().range(node),
      ast().range(ast().declarationOperatorKeyword(node)),
      "%s %s::operator%s(%s)".formatted(
        ast().analyzer().typeOf(node.data().definition(ast())).map(ast()::toString).orElse("<error>"),
        ast().toString(new Expression.Type.Declaration(ast().analyzer().declarationOf(node).id())),
        Ast.operationZ8String(node.data().operation()),
        node.data().parameters(ast()).map(it -> "%s %s".formatted(
          ast().analyzer().typeOf(it.data().definition(ast())).map(ast()::toString).orElse("<error>"),
          it.data().name(ast())
        )).collect(joining(", "))
      )
    );
    node.data()
      .annotations(ast())
      .filter(it -> it.data().name(ast()).equals(Annotation.DEPRECATED))
      .findFirst()
      .ifPresent(_ -> ds.setTags(List.of(SymbolTag.Deprecated)));
    setDocumentSymbol(ds);
  }

  @Override
  protected void declarationRecords(Node<Declaration.Records> node) {
    setDocumentSymbol(new DocumentSymbol(
      "records",
      SymbolKind.Namespace,
      ast().range(node),
      ast().range(ast().declarationRecordsKeyword(node)),
      null,
      ast().siblings(node.data().childrenId())
        .mapToObj(this::visitDocumentSymbol)
        .toList()
    ));
  }

  @Override
  protected void declarationRecordsChild(Node<Declaration.Records.Child> node) {
    setDocumentSymbol(new DocumentSymbol(
      node.data().name(ast()),
      SymbolKind.Constant,
      ast().range(node),
      ast().range(ast().declarationRecordsChildName(node)),
      "guid %s::records::%s".formatted(
        ast().toString(new Expression.Type.Declaration(ast().analyzer().declarationOf(node).id())),
        node.data().name(ast())
      )
    ));
  }

  @Override
  protected void enumerationChild(Node<Enumeration.Child> node) {
    setDocumentSymbol(new DocumentSymbol(
      node.data().name(ast()),
      SymbolKind.EnumMember,
      ast().range(node),
      ast().range(ast().enumerationChildName(node)),
      "enum %s::%s".formatted(
        ast().<org.zenframework.z8.blls.ast.Enumeration>parent(node.id()).data().name(ast()),
        node.data().name(ast())
      )
    ));
  }
}
