import org.jspecify.annotations.NullMarked;

@NullMarked
module org.zenframework.z8.blls.astmemory {
  requires transitive org.zenframework.z8.blls.ast;
  requires org.zenframework.z8.blls.analyzer;
  requires transitive tools.jackson.databind;

  exports org.zenframework.z8.blls.astmemory;
}
