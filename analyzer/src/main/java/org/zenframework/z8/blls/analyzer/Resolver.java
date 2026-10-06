package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.ast.Node;

import java.util.Iterator;
import java.util.Optional;

/// Класс отвечает за разрешение типов с полными имёнами
public abstract class Resolver {
  public static Optional<Node<?>> runSimple(org.zenframework.z8.blls.ast.Ast ast, Iterator<String> values) {
    var r = new Resolver() {
      @Nullable Node<?> result;

      @Override
      protected org.zenframework.z8.blls.ast.Ast ast() {
        return ast;
      }

      @Override
      protected void absent(Ast.Pack pack, String value) {
      }

      @Override
      protected void excess(Node<?> node, String value) {
      }

      @Override
      protected void lack(Ast.Pack pack) {
      }

      @Override
      protected void success(Node<?> node) {
        this.result = node;
      }
    };
    r.run(Ast.Pack.ROOT, values);
    //noinspection NullableProblems
    return Optional.ofNullable(r.result);
  }

  protected abstract org.zenframework.z8.blls.ast.Ast ast();

  /// Пакет `pack` не содержит пакета/типа `value`
  protected abstract void absent(Ast.Pack pack, String value);

  /// Разрешение дошло до `node`, но строки не закончились
  protected abstract void excess(Node<?> node, String value);

  /// Разрешение закончилось на пакете `pack`
  protected abstract void lack(Ast.Pack pack);

  /// Разрешение закончилось успешно
  protected abstract void success(Node<?> node);

  public final void run(Ast.Pack pack, Iterator<String> values) {
    if (!values.hasNext()) {
      // Разрешение остановилось на пакете
      // Не хватает строк в имени
      lack(pack);
      return;
    }

    String v = values.next();
    switch (ast().packGet(pack, v).orElse(null)) {
      // Разрешение остановилось на пакете
      // Отсутствует узел с текущем именем
      case null -> absent(pack, v);
      // Разрешение продолжается в пакете
      case Ast.Pack v2 -> run(v2, values);
      case Ast.PackChild v2 -> {
        if (values.hasNext()) {
          // Разрешение остановилось на объявлении
          // Слишком много строк в имени
          excess(ast().node(v2.id()), values.next());
        } else {
          // Разрешение остановилось на объявлении
          // Строки в имени тоже закончились
          success(ast().node(v2.id()));
        }
      }
    }
  }
}
