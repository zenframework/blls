plugins {
  id("library-conventions")
  alias(libs.plugins.jflex)
}

dependencies {
  api(project(":ast"))
}

sourceSets.main {
  java.srcDir(layout.buildDirectory.dir("generated/sources/main"))
}

tasks.register<Exec>("generateBison") {
  inputs.file("src/main/bison/bl.y")
  val output = layout.buildDirectory.file("generated/sources/main/org/zenframework/z8/blls/compiler/Parser.java").get()
  outputs.file(output)
  commandLine("bison", "src/main/bison/bl.y", "--output", output.asFile.absolutePath)
}

project.afterEvaluate {
  tasks["generateJflex"].dependsOn("generateBison")
}
