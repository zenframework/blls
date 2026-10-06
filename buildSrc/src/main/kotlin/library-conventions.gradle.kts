plugins {
  `java-library`
}

repositories {
  mavenCentral()
}

tasks.compileJava {
  options.javaModuleVersion = version as String
}
