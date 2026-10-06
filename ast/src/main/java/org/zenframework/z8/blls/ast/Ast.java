package org.zenframework.z8.blls.ast;

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Ast {
  String string(int stringId);

  Stream<Token> tokensBack(int tokenId);

  Stream<Token> tokensBackWhitespace(int tokenId);

  Stream<Token> tokensNextWhitespace(int tokenId);

  Stream<Token> tokensNext(int tokenId);

  default Token token(int tokenId) {
    return tokensNext(tokenId).findFirst().orElseThrow();
  }

  Node.Data data(int nodeId);

  @SuppressWarnings("unchecked")
  default <D extends Node.Data> Node<D> node(int nodeId) {
    return new Node<>(nodeId, (D) data(nodeId));
  }

  Token start(Node<?> node);

  Token end(Node<?> node);

  IntStream parents(int nodeId);

  default <D extends Node.Data> Node<D> parent(int nodeId) {
    return parents(nodeId)
      .skip(1)
      .<Node<D>>mapToObj(this::node)
      .findFirst()
      .orElseThrow();
  }

  IntStream siblings(int nodeId);

  Optional<Expression.Type> type(Node<? extends Expression> node);

  <E> E extra(int nodeId, Class<E> klass);

  void extraSet(int nodeId, Object value);

  Optional<Ast.PackChild> packGet(Ast.Pack pack, String value);

  Stream<Map.Entry<String, Ast.PackChild>> packChildren(Ast.Pack pack);

  sealed interface PackChild {
    int id();
  }

  abstract class Delegating implements Ast {
    private final Ast ast;

    protected Delegating(Ast ast) {
      this.ast = ast;
    }

    @Override
    public String string(int stringId) {
      return ast.string(stringId);
    }

    @Override
    public Stream<Token> tokensBack(int tokenId) {
      return ast.tokensBack(tokenId);
    }

    @Override
    public Stream<Token> tokensBackWhitespace(int tokenId) {
      return ast.tokensBackWhitespace(tokenId);
    }

    @Override
    public Stream<Token> tokensNextWhitespace(int tokenId) {
      return ast.tokensNextWhitespace(tokenId);
    }

    @Override
    public Stream<Token> tokensNext(int tokenId) {
      return ast.tokensNext(tokenId);
    }

    @Override
    public Node.Data data(int nodeId) {
      return ast.data(nodeId);
    }

    @Override
    public Token start(Node<?> node) {
      return ast.start(node);
    }

    @Override
    public Token end(Node<?> node) {
      return ast.end(node);
    }

    @Override
    public IntStream parents(int nodeId) {
      return ast.parents(nodeId);
    }

    @Override
    public IntStream siblings(int nodeId) {
      return ast.siblings(nodeId);
    }

    @Override
    public Optional<Expression.Type> type(Node<? extends Expression> node) {
      return ast.type(node);
    }

    @Override
    public <E> E extra(int nodeId, Class<E> klass) {
      return ast.extra(nodeId, klass);
    }

    @Override
    public void extraSet(int nodeId, Object value) {
      ast.extraSet(nodeId, value);
    }

    @Override
    public Optional<PackChild> packGet(Pack pack, String value) {
      return ast.packGet(pack, value);
    }

    @Override
    public Stream<Map.Entry<String, PackChild>> packChildren(Pack pack) {
      return ast.packChildren(pack);
    }
  }

  record Pack(int id) implements PackChild {
    public static final Ast.Pack ROOT = new Ast.Pack(-1);

    public Pack {
      assert id < 0;
    }
  }

  record Declaration(int id) implements PackChild {
    public Declaration {
      assert id > 0;
    }

    public Node<org.zenframework.z8.blls.ast.Declaration> node(Ast ast) {
      return ast.node(id());
    }
  }

  record Enumeration(int id) implements PackChild {
    public Enumeration {
      assert id > 0;
    }

    public Node<org.zenframework.z8.blls.ast.Enumeration> node(Ast ast) {
      return ast.node(id());
    }
  }
}
