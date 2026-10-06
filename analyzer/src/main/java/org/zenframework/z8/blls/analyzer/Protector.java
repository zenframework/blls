package org.zenframework.z8.blls.analyzer;

import org.zenframework.z8.blls.ast.Declaration;
import org.zenframework.z8.blls.ast.Node;

import java.util.stream.Stream;

import static org.zenframework.z8.blls.ast.Declaration.*;

/// Класс отвечает за доступ к элементам объявлений в объектном контексте с учётом их защиты
public abstract class Protector<T> {
  protected abstract Ast ast();

  protected abstract Node<Declaration> declaration();

  protected abstract int modifiers(T value);

  protected final int protection(T value) {
    return Integer.highestOneBit((modifiers(value) & (PROTECTED | PRIVATE)) | PUBLIC);
  }

  /// Поиск нужного элемента в объявлении
  protected abstract Stream<? extends T> search(Node<Declaration> node);

  /// Доступ осуществлён
  protected abstract void success(Stream<? extends T> result);

  public final void run(Node<Declaration> node) {
    // Для проверок на доступ используются PUBLIC, PROTECTED и PRIVATE из перечисления Modifier
    //
    // Если уровень доступа ниже (<) уровня доступа элемента - пропустить

    int protectionFirst;
    if (ast().nesting(declaration()).anyMatch(node::equals))
      // При доступе из Expression.Object, доступ PRIVATE выдаётся также всем
      // Declaration, в которых вложен текущий Expresison.Object
      protectionFirst = PRIVATE;
    else if (ast().nesting(declaration())
      .flatMap(it -> ast().inheritance(it).skip(1))
      .anyMatch(node::equals))
      // Аналогично условию выше + добавляются родительские классы
      protectionFirst = PROTECTED;
    else
      // Иначе - только PUBLIC элементы
      protectionFirst = PUBLIC;

    // Снизить уровень доступа до PROTECTED при доступе к родительским классам
    int protectionHigher = protectionFirst == PRIVATE ? PROTECTED : protectionFirst;

    success(
      Stream.concat(
        search(node)
          .filter(it -> protectionFirst >= protection(it)),
        ast().inheritance(node)
          .skip(1)
          .flatMap(this::search)
          .filter(it -> protectionHigher >= protection(it))
      )
    );
  }
}
