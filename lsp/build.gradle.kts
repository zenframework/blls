plugins {
  id("lsp4j-conventions")
}

dependencies {
  api(project(":ast"))
  api(project(":astmemory"))
  implementation(project(":compiler"))
  implementation(project(":analyzer"))
  implementation(project(":format"))
  implementation(project(":evalsql"))
  api(libs.lsp4j)
}
