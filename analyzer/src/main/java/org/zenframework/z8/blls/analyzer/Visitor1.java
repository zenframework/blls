package org.zenframework.z8.blls.analyzer;

import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

import java.util.Optional;

/// Шаг 1:
/// - Разрешить ссылки для включений
/// - Разрешить все ссылки
public abstract class Visitor1 extends Visitor {
  @Override
  protected abstract Ast ast();

  protected abstract Optional<Expression.Type.Declaration> primaryOf(String name);

  /// `pack`: не содержит `node`.name
  protected abstract void diagnosticDefinitionReferenceAbsent(Node<Definition.Reference> node, Ast.Pack pack);

  /// `result`.name: разрешение достигло объявления
  protected abstract void diagnosticDefinitionReferenceExcess(Node<Definition.Reference> node, Node<?> result);

  /// Не удалось разрешить
  protected abstract void diagnosticDefinitionReferenceLack(Node<Definition.Reference> node, Ast.Pack pack);

  @Override
  protected void import_(Node<Import> node) {
    var reference = node.data().value(ast());
    while (reference.data().childId() > 0) {
      reference = reference.data().child(ast()).orElseThrow();
    }

    var parents = ast().parents(reference.id()).iterator();
    int id = parents.next();

    var pack = Ast.Pack.ROOT;
    loop:
    while (ast().data(id) instanceof Definition.Reference v) {
      var n = new Node<>(id, v);

      switch (ast().packGet(pack, v.name(ast())).orElse(null)) {
        case null -> {
          diagnosticDefinitionReferenceAbsent(n, pack);
          break loop;
        }
        case Ast.Pack v2 -> {
          ast().extraUpdate(n, Extra.DefinitionReference.class, e -> e.setResolved(v2));

          pack = v2;
          id = parents.next();
        }
        case Ast.PackChild v2 -> {
          ast().extraUpdate(n, Extra.DefinitionReference.class, e -> e.setResolved(v2));

          id = parents.next();
          if (ast().data(id) instanceof Definition.Reference) {
            // excess
            diagnosticDefinitionReferenceExcess(n, ast().node(v2.id()));
          }

          break loop;
        }
      }
    }

    switch (ast().extra(node.data().value(ast()), Extra.DefinitionReference.class).getResolved(ast()).orElse(null)) {
      case null -> {
      }
      case Ast.Pack v -> diagnosticDefinitionReferenceLack(node.data().value(ast()), v);
      case Ast.PackChild v -> ast().extraUpdate(node, Extra.Import.class, e -> e.resolvedId = v.id());
    }
  }

  @Override
  protected void definitionReference(Node<Definition.Reference> node) {
    super.definitionReference(node);

    if (node.data().childId() == 0) {
      new ResolverUnqualified() {
        @Override
        protected Ast ast() {
          return Visitor1.this.ast();
        }

        @Override
        protected Optional<Expression.Type.Declaration> primaryOf(String name) {
          return Visitor1.this.primaryOf(name);
        }

        @Override
        protected Node<?> node() {
          return node;
        }

        @Override
        protected String name() {
          return node.data().name(ast());
        }

        @Override
        protected void absent() {
          diagnosticDefinitionReferenceAbsent(node, Ast.Pack.ROOT);
        }

        @Override
        protected void success(Ast.PackChild result) {
          ast().extraUpdate(node, Extra.DefinitionReference.class, e -> e.setResolved(result));

          if (result instanceof Ast.Pack v && !(ast().parent(node.id()).data() instanceof Definition.Reference)) {
            diagnosticDefinitionReferenceLack(node, v);
          }
        }

        @Override
        protected void successImport(Node<Import> node2) {
          ast().extraUpdate(node2, Extra.Import.class, e -> e.used = true);

          ast().extraUpdate(node, Extra.DefinitionReference.class,
            e -> e.resolvedId = ast().extra(node2, Extra.Import.class).resolvedId
          );
        }
      }.run();
    } else {
      switch (ast().extra(node.data().child(ast()).orElseThrow(), Extra.DefinitionReference.class).getResolved(ast()).orElse(null)) {
        case null -> {
        }
        case Ast.Pack v -> {
          switch (ast().packGet(v, node.data().name(ast())).orElse(null)) {
            case null -> diagnosticDefinitionReferenceAbsent(node, v);
            case Ast.PackChild v2 -> {
              ast().extraUpdate(node, Extra.DefinitionReference.class, e -> e.setResolved(v2));

              if (v2 instanceof Ast.Pack v3 && !(ast().parent(node.id()).data() instanceof Definition.Reference)) {
                diagnosticDefinitionReferenceLack(node, v3);
              }
            }
          }
        }
        case Ast.PackChild v -> diagnosticDefinitionReferenceExcess(node, ast().node(v.id()));
      }
    }
  }
}
