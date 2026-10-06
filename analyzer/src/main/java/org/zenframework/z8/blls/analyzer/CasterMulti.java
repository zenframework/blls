package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.Nullable;
import org.zenframework.z8.blls.ast.Declaration;
import org.zenframework.z8.blls.ast.Expression;
import org.zenframework.z8.blls.ast.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;

/// Используется при приведении типов аргументов к ожидаемым параметрам
public abstract class CasterMulti<S extends CasterMulti.SourceProxy, T> {
  protected abstract Ast ast();

  protected abstract Node<Declaration> declaration();

  protected abstract Stream<Expression.Type> parameters(T target);

  /// Приведение невозможно
  protected abstract void absent();

  /// Приведение невозможно: несколько целей подходят
  protected abstract void ambiguous(Stream<T> result);

  /// Приведение выполнено
  protected abstract void success(T result);

  public final void run(List<? extends S> sources, Stream<? extends T> targets) {
    //noinspection NullableProblems
    List<Map.Entry<T, List<CastResult>>> distancesMatrix = targets.map(it -> {
      assert sources.size() == parameters(it).count();

      List<CastResult> matches = new ArrayList<>(sources.size());
      var iter = parameters(it).iterator();
      for (int i = 0; iter.hasNext(); i++) {
        var cr = new CastResult();
        cr.run(sources.get(i).type(), iter.next());
        // Если хоть 1 аргумент не удалось привести - забить
        if (cr.distance < 0) return null;
        matches.add(cr);
      }
      return Map.<T, List<CastResult>>entry(it, matches);
    }).filter(Objects::nonNull).toList();

    var distancesMatrixNoCast = distancesMatrix.stream()
      .filter(it -> it.getValue().stream().allMatch(it2 -> it2.cast == null))
      .toList();

    if (distancesMatrixNoCast.isEmpty()) {
      // Все методы доступны через операторы приведения
      switch (distancesMatrix.size()) {
        case 0 -> absent();
        case 1 -> {
          var sourcesIter = sources.iterator();
          var castResults = distancesMatrix.getFirst().getValue().iterator();

          while (sourcesIter.hasNext()) {
            var s = sourcesIter.next();
            var cr = castResults.next();
            if (cr.cast != null) {
              // Не забыть об использованных операторах приведения
              s.setCast(cr.cast);
            }
          }

          success(distancesMatrix.getFirst().getKey());
        }
        default -> ambiguous(distancesMatrix.stream().map(Map.Entry::getKey));
      }
    } else {
      // Есть методы, доступные без использования операторов приведения

      // Имеем следующую таблицу аргументов, доступных методов
      // и "расстояний" между типами аргументов и типами параметров

      // Пример:
      //        | arg1 | arg2 | arg3
      // -------+------+------+------
      // proxy1 |    0 |    1 |    2
      // proxy2 |    1 |    1 |    3
      // proxy3 |    0 |    2 |    3

      // Сначала пройдёмся по аргументам и выявим наиболее подходящие методы
      // По arg1 - proxy1, proxy3
      // По arg2 - proxy1, proxy2
      // По arg3 - proxy1

      List<List<Map.Entry<T, List<CastResult>>>> bestMatchesByArgs = IntStream.range(0, sources.size()).mapToObj(i -> {
        int lowestDistance = distancesMatrixNoCast.stream()
          .mapToInt(it -> it.getValue().get(i).distance)
          .min()
          .getAsInt();
        return distancesMatrixNoCast.stream()
          .filter(it -> it.getValue().get(i).distance == lowestDistance)
          .toList();
      }).toList();

      // Для arg3 был найден единственный подходящий метод - proxy1
      // (будет хотя бы 1 наиболее подходящий метод)
      var leastCandidate = bestMatchesByArgs.stream().min(comparingInt(List::size));
      if (leastCandidate.isEmpty()) {
        // Если метод без аргументов

        // Для 1 метода - успех
        if (distancesMatrix.size() == 1) success(distancesMatrix.getFirst().getKey());
          // Для нескольких методов - ошибка в разрешении
        else ambiguous(distancesMatrixNoCast.stream().map(Map.Entry::getKey));
      } else if (leastCandidate.get().size() != 1) {
        // Если больше 1 наиболее подходящего метода - ошибка в разрешении
        ambiguous(distancesMatrixNoCast.stream().map(Map.Entry::getKey));
      } else {
        Map.Entry<T, List<CastResult>> candidate = leastCandidate.get().getFirst();
        // Необходимо проверить, что proxy1 также присутствует как подходящий в других аргументах
        // Устанавливать операторы приведения не нужно, т.к. они не используются
        boolean valid = bestMatchesByArgs.stream().allMatch(it ->
          // Искать требуется именно proxy-объект
          it.stream()
            .map(Map.Entry::getKey)
            .anyMatch(it2 -> it2.equals(candidate.getKey()))
        );
        if (valid)
          success(candidate.getKey());
        else
          // В противном случае - ошибка в разрешении
          ambiguous(distancesMatrixNoCast.stream().map(Map.Entry::getKey));
      }
    }
  }

  public interface SourceProxy {
    Expression.Type type();

    void setCast(Node<Declaration.Cast> node);
  }

  private final class CastResult extends CasterEnhanced {
    private int distance = -1;
    private @Nullable Node<Declaration.Cast> cast = null;

    @Override
    protected Ast ast() {
      return CasterMulti.this.ast();
    }

    @Override
    protected Node<Declaration> declaration() {
      return CasterMulti.this.declaration();
    }

    @Override
    protected void failed() {
    }

    @Override
    protected void success(int distance) {
      this.distance = distance;
    }

    @Override
    protected void success() {
      this.distance = 0;
    }

    @Override
    protected void success(Node<Declaration.Cast> cast) {
      this.distance = 0;
      this.cast = cast;
    }
  }
}
