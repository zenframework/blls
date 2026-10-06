plugins {
  id("library-conventions")
  id("org.gradlex.extra-java-module-info")
}

extraJavaModuleInfo {
  failOnAutomaticModules = true
  module("org.eclipse.lsp4j:org.eclipse.lsp4j", "org.eclipse.lsp4j") {
    requires("com.google.gson")
    requires("org.eclipse.lsp4j.jsonrpc")

    exports("org.eclipse.lsp4j")
    exports("org.eclipse.lsp4j.adapters")
    exports("org.eclipse.lsp4j.launch")
    exports("org.eclipse.lsp4j.services")
    exports("org.eclipse.lsp4j.util")

    opens("org.eclipse.lsp4j", "com.google.gson", "org.eclipse.lsp4j.jsonrpc")
  }
  module("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc", "org.eclipse.lsp4j.jsonrpc") {
    requires("java.logging")
    requires("com.google.gson")

    exports("org.eclipse.lsp4j.jsonrpc")
    exports("org.eclipse.lsp4j.jsonrpc.json")
    exports("org.eclipse.lsp4j.jsonrpc.json.adapters")
    exports("org.eclipse.lsp4j.jsonrpc.messages")
    exports("org.eclipse.lsp4j.jsonrpc.services")
    exports("org.eclipse.lsp4j.jsonrpc.util")
    exports("org.eclipse.lsp4j.jsonrpc.validation")

    opens("org.eclipse.lsp4j.jsonrpc.messages", "com.google.gson")
  }
}
