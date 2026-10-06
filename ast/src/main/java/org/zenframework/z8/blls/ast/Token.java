package org.zenframework.z8.blls.ast;

public record Token(int id, Kind kind, int line, int character, int index) {
  @Override
  public boolean equals(Object it) {
    return it instanceof Token v && id() == v.id();
  }

  @Override
  public int hashCode() {
    return id();
  }

  public enum Kind {
    EOF,
    NEW_LINE,
    COMMENT,
    COMMENT_BLOCK,
    SPACE,

    EQUAL,
    PLUS,
    PLUS_EQUAL,
    MINUS,
    MINUS_EQUAL,
    ASTERISK,
    ASTERISK_EQUAL,
    SLASH,
    SLASH_EQUAL,
    PERCENT,
    PERCENT_EQUAL,
    EQUAL_EQUAL,
    EXCLAMATION_EQUAL,
    ARROW_R,
    ARROW_R_EQUAL,
    ARROW_L,
    ARROW_L_EQUAL,
    EXCLAMATION,
    AMPERSAND,
    AMPERSAND_AMPERSAND,
    PIPE,
    PIPE_PIPE,
    TILDA,
    ACCENT,
    COMMA,
    DOT,
    BRACE_L,
    BRACE_R,
    BRACKET_L,
    BRACKET_R,
    PAREN_L,
    PAREN_R,
    QUESTION,
    COLON,
    QUESTION_COLON,
    SEMICOLON,

    KEYWORD_NULL,
    KEYWORD_TRUE,
    KEYWORD_FALSE,
    KEYWORD_INSTANCEOF,
    KEYWORD_NEW,
    KEYWORD_BREAK,
    KEYWORD_CONTINUE,
    KEYWORD_RETURN,
    KEYWORD_IF,
    KEYWORD_ELSE,
    KEYWORD_DO,
    KEYWORD_WHILE,
    KEYWORD_FOR,
    KEYWORD_THROW,
    KEYWORD_TRY,
    KEYWORD_CATCH,
    KEYWORD_FINALLY,
    KEYWORD_IMPORT,
    KEYWORD_CLASS,
    KEYWORD_EXTENDS,
    KEYWORD_ENUM,
    KEYWORD_RECORDS,
    KEYWORD_OPERATOR,
    KEYWORD_THIS,
    KEYWORD_SUPER,
    KEYWORD_CONTAINER,
    KEYWORD_PUBLIC,
    KEYWORD_PROTECTED,
    KEYWORD_PRIVATE,
    KEYWORD_STATIC,
    KEYWORD_FINAL,
    KEYWORD_VIRTUAL,
    KEYWORD_AUTO,

    INTEGER,
    DECIMAL,
    STRING,
    SPECIAL,
    IDENTIFIER,
  }
}
