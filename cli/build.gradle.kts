plugins {
  application
  id("lsp4j-conventions")
}

dependencies {
  implementation(project(":compiler"))
  implementation(project(":analyzer"))
  implementation(project(":format"))
  implementation(project(":lsp"))
  implementation(libs.picocli)
}

application {
  mainModule = "org.zenframework.z8.blls"
  mainClass = "org.zenframework.z8.blls.Main"
}

tasks.compileJava {
  options.javaModuleVersion = version as String
}

tasks.run {
  enableAssertions = true
}

tasks.register("printRuntimeClasspath") {
  println(sourceSets.main.get().runtimeClasspath.asPath)
}
