package org.zenframework.z8.blls.lsp;

import org.zenframework.z8.blls.analyzer.Ast.Extra;
import org.zenframework.z8.blls.ast.*;

abstract class VisitorImplementation extends VisitorHit {
  protected abstract void success(Node<?> node);

  protected abstract void visitGlobal(VisitorGlobal visitor);

  @Override
  protected void hitKlassName(Node<Klass> node) {
    visitGlobal(new VisitorGlobalExtended(node.data().child(ast())));
  }

  @Override
  protected void hitDeclarationMethodName(Node<Declaration.Method> node) {
    visitGlobal(new VisitorGlobalOverride(node));
  }

  @Override
  protected void hitDeclarationPriorityKeyword(Node<Declaration.Priority> node) {
    visitGlobal(new VisitorGlobalOverride(node));
  }

  @Override
  protected void hitDeclarationCastKeyword(Node<Declaration.Cast> node) {
    visitGlobal(new VisitorGlobalOverride(node));
  }

  @Override
  protected void hitDeclarationCastDefinitionReference(Node<Declaration.Cast> node) {
    visitGlobal(new VisitorGlobalOverride(node));
  }

  @Override
  protected void hitDeclarationOperatorKeyword(Node<Declaration.Operator> node) {
    visitGlobal(new VisitorGlobalOverride(node));
  }

  @Override
  protected void hitDeclarationOperatorOperation(Node<Declaration.Operator> node) {
    visitGlobal(new VisitorGlobalOverride(node));
  }

  @Override
  protected void hitDefinitionReferenceName(Node<Definition.Reference> node) {
    var e = ast().analyzer().extra(node, Extra.DefinitionReference.class);
    if (e.getResolved(ast()).orElse(null) instanceof Ast.Declaration v) {
      visitGlobal(new VisitorGlobalExtended(v.node(ast())));
    }
  }

  sealed abstract static class VisitorGlobal extends Visitor {
    final Node<?> target;

    protected VisitorGlobal(Node<?> target) {
      this.target = target;
    }
  }

  private final class VisitorGlobalExtended extends VisitorGlobal {
    VisitorGlobalExtended(Node<Declaration> target) {
      super(target);
    }

    @Override
    protected Ast ast() {
      return VisitorImplementation.this.ast();
    }

    @Override
    protected void declaration(Node<Declaration> node) {
      super.declaration(node);

      ast().analyzer()
        .inheritance(node)
        .skip(1)
        .findFirst()
        .filter(it -> it.id() == target.id())
        .ifPresent(_ -> success(node));
    }
  }

  private final class VisitorGlobalOverride extends VisitorGlobal {
    VisitorGlobalOverride(Node<? extends Declaration.Child> target) {
      super(target);
    }

    @Override
    protected Ast ast() {
      return VisitorImplementation.this.ast();
    }

    @Override
    protected void declarationMethod(Node<Declaration.Method> node) {
      super.declarationMethod(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationMethod.class);
      if (e.overrideId == target.id()) success(node);
    }

    @Override
    protected void declarationPriority(Node<Declaration.Priority> node) {
      super.declarationPriority(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationPriority.class);
      if (e.overrideId == target.id()) success(node);
    }

    @Override
    protected void declarationCast(Node<Declaration.Cast> node) {
      super.declarationCast(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationCast.class);
      if (e.overrideId == target.id()) success(node);
    }

    @Override
    protected void declarationOperator(Node<Declaration.Operator> node) {
      super.declarationOperator(node);

      var e = ast().analyzer().extra(node, Extra.DeclarationOperator.class);
      if (e.overrideId == target.id()) success(node);
    }
  }
}
