import org.jspecify.annotations.NullMarked;

@NullMarked
module org.zenframework.z8.blls.analyzer {
  requires transitive org.zenframework.z8.blls.ast;

  exports org.zenframework.z8.blls.analyzer;
}
