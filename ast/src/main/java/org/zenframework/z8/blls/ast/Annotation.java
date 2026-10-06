package org.zenframework.z8.blls.ast;

import java.util.Optional;
import java.util.regex.Pattern;

public record Annotation(int nameId, int valueId) implements Node.Data {
  public static final Pattern REGEX_NAME = Pattern.compile("([A-Z_a-z]|[^\\x00-\\x7F])[^\\x00-\\x1F]{0,127}");
  public static final Pattern REGEX_CRON = Pattern.compile("((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?(,((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?)*\\s+((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?(,((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?)*\\s+((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?(,((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?)*\\s+((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?(,((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?)*\\s+((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?(,((\\*)|(\\d{1,2}(-\\d{1,2})?))(/\\d{1,2})?)*");

  public static final String NAME = "name";
  public static final String DISPLAY_NAME = "displayName";

  public static final String ABSTRACT = "abstract";
  public static final String PRIMARY = "primary";
  public static final String NATIVE = "native";
  public static final String GENERATABLE = "generatable";
  public static final String EXECUTABLE = "executable";
  public static final String REQUEST = "request";
  public static final String ACTIVATOR = "activator";
  public static final String ENTRY = "entry";
  public static final String JOB = "job";
  public static final String LOG = "log";
  public static final String UI = "ui";

  public static final String DEPRECATED = "deprecated";
  public static final String ICON = "icon";
  public static final String FOREIGN_KEY = "foreignKey";
  public static final String LOCK_KEY = "lockKey";

  public String name(Ast ast) {
    return ast.string(nameId());
  }

  public Optional<Node<? extends Expression>> value(Ast ast) {
    return valueId() == 0 ? Optional.empty() : Optional.of(ast.node(valueId()));
  }
}
