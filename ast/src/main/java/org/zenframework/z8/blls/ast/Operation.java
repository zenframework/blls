package org.zenframework.z8.blls.ast;

public enum Operation {
  PLUS,
  NEGATE,
  NOT,
  BIT_NOT,

  ELVIS,
  MULTIPLY,
  DIVIDE,
  MODULE,
  ADD,
  SUBTRACT,
  LESS,
  LESS_EQUAL,
  MORE,
  MORE_EQUAL,
  EQUAL,
  NOT_EQUAL,
  AND,
  OR,
  BIT_AND,
  BIT_OR,
  BIT_XOR,
  ASSIGN,
  ASSIGN_ADD,
  ASSIGN_SUBTRACT,
  ASSIGN_MULTIPLY,
  ASSIGN_DIVIDE,
  ASSIGN_MODULE;

  @Override
  public String toString() {
    return switch (this) {
      case PLUS -> "+";
      case NEGATE -> "-";
      case NOT -> "!";
      case BIT_NOT -> "~";
      case ELVIS -> "?:";
      case MULTIPLY -> "*";
      case DIVIDE -> "/";
      case MODULE -> "%";
      case ADD -> "+";
      case SUBTRACT -> "-";
      case LESS -> "<";
      case LESS_EQUAL -> "<=";
      case MORE -> ">";
      case MORE_EQUAL -> ">=";
      case EQUAL -> "==";
      case NOT_EQUAL -> "!=";
      case AND -> "&&";
      case OR -> "||";
      case BIT_AND -> "&";
      case BIT_OR -> "|";
      case BIT_XOR -> "^";
      case ASSIGN -> "=";
      case ASSIGN_ADD -> "+=";
      case ASSIGN_SUBTRACT -> "-=";
      case ASSIGN_MULTIPLY -> "*=";
      case ASSIGN_DIVIDE -> "/=";
      case ASSIGN_MODULE -> "%=";
    };
  }
}
