repositories {
  intellijPlatform {
    defaultRepositories()
  }
}

plugins {
  `java-library`
  alias(libs.plugins.intellij)
  alias(libs.plugins.grammarkit)
}

val grammarKitDir = layout.buildDirectory.dir("generated/sources/grammarkit/java")

sourceSets.main {
  java.srcDirs(grammarKitDir)
}

dependencies {
  intellijPlatform {
    intellijIdea("2026.2")
  }
}

intellijPlatform {
  // Расширение не содержит настроек, можно отключить их поиск
  buildSearchableOptions = false
}

tasks {
  generateLexer {
    sourceFile = file("src/main/grammar/BL.flex")
    targetRootOutputDir.set(file(grammarKitDir))
    purgeOldFiles = true
  }

  generateParser {
    sourceFile = file("src/main/grammar/BL.bnf")
    pathToParser = "org/zenframework/z8/blls/idea/BlParser.java"
    pathToPsiRoot = "org/zenframework/z8/blls/idea/psi"
    targetRootOutputDir.set(file(grammarKitDir))
    purgeOldFiles = true
  }

  compileJava {
    dependsOn("generateLexer")
    dependsOn("generateParser")
  }

  patchPluginXml {
    pluginVersion.set(project.version.toString())
    sinceBuild.set("262")
  }
}
