import org.jspecify.annotations.NullMarked;

@NullMarked
module org.zenframework.z8.blls.lsp {
  requires transitive org.zenframework.z8.blls.ast;
  requires transitive org.zenframework.z8.blls.astmemory;
  requires org.zenframework.z8.blls.compiler;
  requires org.zenframework.z8.blls.analyzer;
  requires org.zenframework.z8.blls.format;
  requires org.zenframework.z8.blls.evalsql;

  requires transitive org.eclipse.lsp4j;
  requires transitive org.eclipse.lsp4j.jsonrpc;

  requires com.google.gson;

  exports org.zenframework.z8.blls.lsp;

  opens org.zenframework.z8.blls.lsp to tools.jackson.databind;
}
