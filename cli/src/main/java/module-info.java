import org.jspecify.annotations.NullMarked;

@NullMarked
module org.zenframework.z8.blls {
  requires org.zenframework.z8.blls.compiler;
  requires org.zenframework.z8.blls.analyzer;
  requires org.zenframework.z8.blls.format;
  requires org.zenframework.z8.blls.lsp;

  requires info.picocli;

  opens org.zenframework.z8.blls to info.picocli;
}
