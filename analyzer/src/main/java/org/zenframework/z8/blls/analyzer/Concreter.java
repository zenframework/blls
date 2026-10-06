package org.zenframework.z8.blls.analyzer;

import org.zenframework.z8.blls.ast.Expression;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/// Коллекция типов, где ни один тип не наследуется от другого
public abstract class Concreter implements Iterable<Concreter.Proxy> {
  private final List<Proxy> result = new ArrayList<>();

  protected abstract Ast ast();

  @Override
  public Iterator<Proxy> iterator() {
    return result.iterator();
  }

  public void add(Proxy source) {
    // Добавлять ли по итогу тип
    final boolean[] verdict = {true};

    var iter = result.listIterator();
    while (iter.hasNext()) {
      var target = iter.next();
      new Caster() {
        @Override
        protected Ast ast() {
          return Concreter.this.ast();
        }

        @Override
        protected void failed() {
          // source и target не являются частью одной иерархии - добавить source
          new Caster() {
            @Override
            protected Ast ast() {
              return Concreter.this.ast();
            }

            @Override
            protected void failed() {
            }

            @Override
            protected void success(int distance) {
              // target более конкретный, чем source - не добавлять
              verdict[0] = false;
            }

            @Override
            protected void success() {
              throw new AssertionError();
            }
          }.run(target.type(), source.type());
        }

        @Override
        protected void success(int distance) {
          // source более конкретный, чем target - убрать target
          iter.remove();
        }

        @Override
        protected void success() {
          // source уже встречается - не добавлять
          verdict[0] = false;
        }
      }.run(source.type(), target.type());
    }

    if (verdict[0])
      iter.add(source);
  }

  public interface Proxy {
    Expression.Type type();
  }
}
