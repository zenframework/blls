package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Import;
import org.zenframework.z8.blls.ast.Node;
import org.zenframework.z8.blls.ast.Source;

import java.util.Optional;

public abstract class ResolverUnqualified {
  protected abstract org.zenframework.z8.blls.ast.Ast ast();

  protected abstract Optional<Expression.Type.Declaration> primaryOf(String name);

  protected abstract Node<?> node();

  protected abstract String name();

  protected abstract void absent();

  protected abstract void success(Ast.PackChild result);

  protected abstract void successImport(Node<Import> node);

  public final void run() {
    var source = ast().parents(node().id())
      .<Node<Source>>mapToObj(ast()::node)
      .reduce((_, b) -> b)
      .orElseThrow();

    // посмотреть в текущем пакете
    Ast.@Nullable PackChild pm = switch (ast().packGet(ast().extra(source.id(), Extra.Source.class).pack(), name()).orElse(null)) {
      case null -> null;
      // находясь в org.zenframework
      // нельзя получить доступ к org.zenframework.z8
      case Ast.Pack _ -> null;
      case Ast.PackChild v -> v;
    };

    if (pm == null) {
      // посмотреть в пакетах
      // org.zenframework.z8.lang
      // org.zenframework.z8.lang.sql
      pm = primaryOf(name()).map(it -> new Ast.Declaration(it.id())).orElse(null);
    }

    // посмотреть корневые типы и пакеты
    if (pm == null) {
      pm = ast().packGet(Ast.Pack.ROOT, name()).orElse(null);
    }

    if (pm instanceof Ast.Pack) {
      success(pm);
    } else {
      var resolvedImport = source.data()
        .imports(ast())
        .filter(it -> it.data().value(ast()).data().name(ast()).equals(name()))
        .findFirst()
        .orElse(null);

      if (pm == null) {
        if (resolvedImport != null) {
          // ничего не найдено в пакетах, найдено во включениях
          successImport(resolvedImport);
        } else {
          // если ничего не найдено ни в пакетах, ни во включениях
          absent();
        }
      } else if (resolvedImport == null) {
        // найдено в пакетах, не найдено во включениях
        success(pm);
      } else {
        var e = ast().extra(resolvedImport.data().valueId(), Extra.DefinitionReference.class);
        if (pm.id() == e.resolvedId) {
          // найдено в пакетах, найдено во включениях
          // значения совпадают, приоритет пакета (т.е. включение "не используется")
          success(pm);
        } else {
          // найдено в пакетах, найдено во включениях
          // значения различны, приоритет включения
          successImport(resolvedImport);
        }
      }
    }
  }
}
