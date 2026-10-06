package org.zenframework.z8.blls.lsp;

import org.zenframework.z8.blls.ast.*;

import java.util.Optional;
import java.util.StringJoiner;

abstract class VisitorDoc extends Visitor {
  private static Optional<String> docOf(String comment) {
    if (!comment.startsWith("/**") || !comment.endsWith("*/"))
      return Optional.empty();

    String content = comment.substring(3, comment.length() - 2);
    var lines = content.lines()
      .map(String::trim)
      .iterator();

    var first = lines.next();

    if (!first.isBlank() || !lines.hasNext())
      return Optional.empty();

    var result = new StringJoiner("\n");
    while (true) {
      String line = lines.next();

      if (lines.hasNext()) {
        line = line.stripLeading();

        if (line.isEmpty() || line.charAt(0) != '*')
          return Optional.empty();

        result.add(line.substring(1).trim());
      } else if (!line.isBlank())
        return Optional.empty();
      else
        return Optional.of(result.toString().trim());
    }
  }

  private Optional<String> docBefore(Token token) {
    return ast().tokensBackWhitespace(token.id())
      .takeWhile(it -> switch (it.kind()) {
        case Token.Kind.NEW_LINE,
             Token.Kind.COMMENT,
             Token.Kind.COMMENT_BLOCK -> true;
        default -> false;
      })
      .filter(it -> it.kind() == Token.Kind.COMMENT_BLOCK)
      .findFirst()
      .map(it -> text().substring(
        it.index(),
        ast().tokensNextWhitespace(it.id())
          .skip(1)
          .findFirst()
          .orElseThrow()
          .index()
      ))
      .flatMap(VisitorDoc::docOf);
  }

  @Override
  protected abstract Ast ast();

  protected abstract String text();

  @Override
  protected void klass(Node<Klass> node) {
    super.klass(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }

  @Override
  protected void enumeration(Node<Enumeration> node) {
    super.enumeration(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }

  @Override
  protected void declarationField(Node<Declaration.Field> node) {
    super.declarationField(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }

  @Override
  protected void declarationMethod(Node<Declaration.Method> node) {
    super.declarationMethod(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }

  @Override
  protected void declarationPriority(Node<Declaration.Priority> node) {
    super.declarationPriority(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }

  @Override
  protected void declarationCast(Node<Declaration.Cast> node) {
    super.declarationCast(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }

  @Override
  protected void declarationOperator(Node<Declaration.Operator> node) {
    super.declarationOperator(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }

  @Override
  protected void declarationRecordsChild(Node<Declaration.Records.Child> node) {
    super.declarationRecordsChild(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }

  @Override
  protected void enumerationChild(Node<Enumeration.Child> node) {
    super.enumerationChild(node);

    docBefore(ast().start(node)).ifPresent(doc ->
      ast().extraUpdate(node, e -> e.doc = doc)
    );
  }
}
