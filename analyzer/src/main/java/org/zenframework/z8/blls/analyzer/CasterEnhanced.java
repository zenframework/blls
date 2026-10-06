package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.ast.Declaration;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Node;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/// Расширяет алгоритм приведения типов, добавляя работу операторов приведения
public abstract class CasterEnhanced extends Caster {
  protected abstract Node<Declaration> declaration();

  /// Приведение выполнено через оператор приведения `node`
  protected abstract void success(Node<Declaration.Cast> node);

  @Override
  protected final boolean run(Expression.Type.Declaration source, Expression.Type.Declaration target) {
    if (super.run(source, target)) return true;

    // Берём только операторы, которые приводят к типу, совместимому с целевым
    // Записываем сведения о расстоянии до target-типа (Map.Entry<Integer, Declaration.Cast>)
    class CasterDistance extends Caster {
      private final Node<Declaration.Cast> cast;
      int result = -1;

      CasterDistance(Node<Declaration.Cast> cast) {
        this.cast = cast;
      }

      @Override
      protected Ast ast() {
        return CasterEnhanced.this.ast();
      }

      @Override
      protected void failed() {
      }

      @Override
      protected void success(int distance) {
        result = distance;
      }

      @Override
      protected void success() {
        result = 0;
      }

      Map.@Nullable Entry<Integer, Node<Declaration.Cast>> run() {
        run(ast().typeOf(cast.data().definition(ast())).orElseThrow(), target);
        return result < 0 ? null : Map.entry(result, cast);
      }
    }

    var accessor = new Accessor<Node<Declaration.Cast>>() {
      boolean result = false;

      @Override
      protected Ast ast() {
        return CasterEnhanced.this.ast();
      }

      @Override
      protected Node<Declaration> declaration() {
        return CasterEnhanced.this.declaration();
      }

      @Override
      protected int modifiers(Node<Declaration.Cast> value) {
        return value.data().modifiers();
      }

      @Override
      protected void absent() {
      }

      @Override
      protected void success2(Stream<? extends Node<Declaration.Cast>> result) {
        var cast = result.map(CasterDistance::new)
          .map(CasterDistance::run)
          .filter(Objects::nonNull)
          // При наличии - выбрать оператор, приводящий к наименьшему расстоянию до цели
          .min(Map.Entry.comparingByKey())
          .map(Map.Entry::getValue);
        cast.ifPresent(CasterEnhanced.this::success);
        this.result = cast.isPresent();
      }

      @Override
      protected Object key(Node<Declaration.Cast> value) {
        return value.data().definitionId();
      }

      @Override
      protected void run(Expression.Type.Enumeration source) {
        absent();
      }

      @Override
      protected void run(Expression.Type.Array target) {
        absent();
      }

      @Override
      protected void run(Expression.Type.Map target) {
        absent();
      }

      // Просто возвращаем все операторы приведения
      @Override
      protected Stream<? extends Node<Declaration.Cast>> search(Node<Declaration> declaration) {
        return declaration.data().children(ast())
          .gather(Ast.only(Declaration.Cast.class))
          .filter(it -> ast().typeOf(it.data().definition(ast())).isPresent());
      }
    };
    accessor.run(ast().node(source.id()));
    return accessor.result;
  }
}
