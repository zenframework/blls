plugins {
  id("library-conventions")
}

dependencies {
  api(project(":ast"))
  implementation(project(":analyzer"))
  api(libs.json)
}
