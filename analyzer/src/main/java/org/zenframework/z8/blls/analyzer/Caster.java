package org.zenframework.z8.blls.analyzer;

import org.zenframework.z8.blls.ast.Declaration;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Node;

/// Класс отвечает за приведение типов
public abstract class Caster {
  public static boolean runSimple(Ast ast, Expression.Type source, Expression.Type target) {
    var caster = new Caster() {
      boolean result = false;

      @Override
      protected Ast ast() {
        return ast;
      }

      @Override
      protected void failed() {
      }

      @Override
      protected void success(int distance) {
        result = true;
      }

      @Override
      protected void success() {
        result = true;
      }
    };
    caster.run(source, target);
    return caster.result;
  }

  protected abstract Ast ast();

  /// Приведение невозможно
  protected abstract void failed();

  /// Приведение выполнено: исходный тип наследует целевой с разницей в `distance` поколений
  protected abstract void success(int distance);

  /// Приведение выполнено
  protected abstract void success();

  public final void run(Expression.Type source, Expression.Type target) {
    if (source instanceof Expression.Type.Declaration v1 && target instanceof Expression.Type.Declaration v2 && run(v1, v2))
      return;

    // Обработать другие случаи (например массив и массив)
    if (compatible(source, target) == -1) failed();
    else success();
  }

  // Логика переопределяется в CasterEnhanced
  protected boolean run(Expression.Type.Declaration source, Expression.Type.Declaration target) {
    // Проверить, наследуется ли один тип от другого
    int v = compatibleAncestor(source.node(ast()), target.node(ast()));
    if (v == -1) {
      return false;
    } else {
      // Наследование есть
      success(v);
      return true;
    }
  }

  // FIXME: приведение string[] к primary[] черевато ClassCastException. Нужны wildcardы
  private int compatible(Expression.Type source, Expression.Type target) {
    return switch (source) {
      case Expression.Type.Null _ -> 0;
      case Expression.Type.Declaration v1 -> target instanceof Expression.Type.Declaration v2
        ? compatibleAncestor(v1.node(ast()), v2.node(ast()))
        : -1;
      case Expression.Type.Enumeration(int id) ->
        target instanceof Expression.Type.Enumeration(int id2) && id == id2 ? 0 : -1;
      case Expression.Type.Array v -> target instanceof Expression.Type.Array v2 ? compatibleArray(v, v2) : -1;
      case Expression.Type.Map v -> target instanceof Expression.Type.Map v2 ? compatibleMap(v, v2) : -1;
    };
  }

  // Проверка совместимости дочерних типов (сведения о расстоянии при этом теряется)
  private int compatibleArray(Expression.Type.Array source, Expression.Type.Array target) {
    return compatible(source.value(), target.value()) == -1 ? -1 : 0;
  }

  // Проверка совместимости ключей и значений (сведения о расстоянии при этом теряется)
  private int compatibleMap(Expression.Type.Map source, Expression.Type.Map target) {
    return compatible(source.key(), target.key()) == -1 || compatible(source.value(), target.value()) == -1 ? -1 : 0;
  }

  private int compatibleAncestor(Node<Declaration> source, Node<Declaration> target) {
    int distance = 0;
    var iter = ast().inheritance(source).iterator();
    while (iter.hasNext()) {
      if (iter.next().equals(target)) return distance;
      distance++;
    }
    return -1;
  }
}
