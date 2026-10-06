package org.zenframework.z8.blls.analyzer;

import org.zenframework.z8.blls.ast.*;

import static java.util.function.Predicate.not;

/// Класс отвечает за доступ к переменным
abstract class Variabler {
  protected abstract Ast ast();

  /// Переменная не найдена
  protected abstract void absent();

  protected abstract void successParameter(Node<Parameter> node);

  protected abstract void successStatementVariable(Node<Statement.Variable> node);

  protected abstract void successStatementLoopForEach(Node<Statement.LoopForEach> node);

  protected abstract void successStatementTryCatch(Node<Statement.Try.Catch> node);

  final void run(Node<?> node, int index, String value) {
    var iter = ast().parents(node.id())
      .mapToObj(ast()::node)
      .iterator();

    loop:
    while (iter.hasNext()) {
      var v = iter.next();
      switch (v.data()) {
        case Klass _, Enumeration _ -> {
          break loop;
        }
        case Declaration.Method v2 -> {
          var v3 = v2.parameters(ast())
            .filter(it -> it.data().name(ast()).equals(value))
            .findFirst();
          if (v3.isPresent()) {
            successParameter(v3.get());
            return;
          }
        }
        case Declaration.Operator v2 -> {
          var v3 = v2.parameters(ast())
            .filter(it -> it.data().name(ast()).equals(value))
            .findFirst();
          if (v3.isPresent()) {
            successParameter(v3.get());
            return;
          }
        }
        case Statement.Block v2 -> {
          var v3 = v2.statements(ast())
            .gather(Ast.only(Statement.Variable.class))
            .filter(it -> it.data().name(ast()).equals(value))
            .findFirst()
            // Чтобы исключить
            // println(a);
            // int a;
            .filter(it -> ast().start(it).index() < index)
            // Чтобы исключить
            // int a = a;
            .filter(not(it -> ast().parents(node.id()).anyMatch(it2 -> it2 == it.id())));
          if (v3.isPresent()) {
            successStatementVariable(v3.get());
            return;
          }
        }
        case Statement.LoopFor v2 -> {
          var v3 = v2.initializer(ast())
            .stream()
            .gather(Ast.only(Statement.Variable.class))
            .findFirst()
            .filter(it -> it.data().name(ast()).equals(value))
            // Чтобы исключить
            // for (int a = a; ...
            .filter(not(it -> ast().parents(node.id()).anyMatch(it2 -> it2 == it.id())));
          if (v3.isPresent()) {
            successStatementVariable(v3.get());
            return;
          }
        }
        case Statement.LoopForEach v2 -> {
          if (v2.name(ast()).equals(value)) {
            successStatementLoopForEach(new Node<>(v.id(), v2));
            return;
          }
        }
        case Statement.Try.Catch v2 -> {
          if (v2.name(ast()).equals(value)) {
            successStatementTryCatch(new Node<>(v.id(), v2));
            return;
          }
        }
        case Annotation _,
             Declaration _,
             Declaration.Field _,
             Declaration.Initialize _,
             Declaration.Priority _,
             Declaration.Cast _,
             Declaration.Records _,
             Declaration.Records.Child _,
             Statement.Expression _,
             Statement.Variable _,
             Statement.Return _,
             Statement.Break _,
             Statement.Continue _,
             Statement.Throw _,
             Statement.Try _,
             Statement.Condition _,
             Statement.LoopDo _,
             Statement.LoopWhile _,
             Expression.Paren _,
             Expression.Null _,
             Expression.Boolean _,
             Expression.Integer _,
             Expression.Decimal _,
             Expression.String _,
             Expression.Special _,
             Expression.This _,
             Expression.Super _,
             Expression.Container _,
             Expression.New _,
             Expression.Array _,
             Expression.Access _,
             Expression.Index _,
             Expression.Invoke _,
             Expression.Cast _,
             Expression.Object _,
             Expression.Unary _,
             Expression.Binary _,
             Expression.Instanceof _,
             Expression.Ternary _,
             Expression.Array.Child _,
             Expression.Invoke.Argument _ -> {
        }
        case Definition _,
             Enumeration.Child _,
             Import _,
             Parameter _,
             Source _ -> throw new AssertionError();
      }
    }

    absent();
  }
}
