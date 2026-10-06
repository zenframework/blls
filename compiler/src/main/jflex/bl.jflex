package org.zenframework.z8.blls.compiler;

import module org.zenframework.z8.blls.ast;

%%

%public
%abstract
%class Lexer
%implements Parser.Lexer
%unicode
%char
%line
%column
%integer

%{
  private StringBuilder string = new StringBuilder();
  private int commentBlockLevel = 0;
  private int tokenId = 0;
  private Object lval = null;

  @Override
  public final Integer getStartPos() { return tokenId; }

  @Override
  public final Integer getEndPos() { return tokenId; }

  @Override
  public final Object getLVal() {
    Object result = lval;
    lval = null;
    return result;
  }

  protected abstract int stringSet(String value);

  protected abstract int tokenSet(Token value);

  private void tokenSet(Token.Kind kind) {
    tokenId = tokenSet(new Token(0, kind, yyline, yycolumn, (int) yychar));
  }
%}

%x X_STRING
%x X_SPECIAL
%x X_COMMENT_BLOCK

%%

"="
  { tokenSet(Token.Kind.EQUAL);               return EQUAL; }
"+"
  { tokenSet(Token.Kind.PLUS);                return PLUS; }
"+="
  { tokenSet(Token.Kind.PLUS_EQUAL);          return PLUS_EQUAL; }
"-"
  { tokenSet(Token.Kind.MINUS);               return MINUS; }
"-="
  { tokenSet(Token.Kind.MINUS_EQUAL);         return MINUS_EQUAL; }
"*"
  { tokenSet(Token.Kind.ASTERISK);            return ASTERISK; }
"*="
  { tokenSet(Token.Kind.ASTERISK_EQUAL);      return ASTERISK_EQUAL; }
"/"
  { tokenSet(Token.Kind.SLASH);               return SLASH; }
"/="
  { tokenSet(Token.Kind.SLASH_EQUAL);         return SLASH_EQUAL; }
"%"
  { tokenSet(Token.Kind.PERCENT);             return PERCENT; }
"%="
  { tokenSet(Token.Kind.PERCENT_EQUAL);       return PERCENT_EQUAL; }
"=="
  { tokenSet(Token.Kind.EQUAL_EQUAL);         return EQUAL_EQUAL; }
"!="
  { tokenSet(Token.Kind.EXCLAMATION_EQUAL);   return EXCLAMATION_EQUAL; }
">"
  { tokenSet(Token.Kind.ARROW_R);             return ARROW_R; }
">="
  { tokenSet(Token.Kind.ARROW_R_EQUAL);       return ARROW_R_EQUAL; }
"<"
  { tokenSet(Token.Kind.ARROW_L);             return ARROW_L; }
"<="
  { tokenSet(Token.Kind.ARROW_L_EQUAL);       return ARROW_L_EQUAL; }
"!"
  { tokenSet(Token.Kind.EXCLAMATION);         return EXCLAMATION; }
"&"
  { tokenSet(Token.Kind.AMPERSAND);           return AMPERSAND; }
"&&"
  { tokenSet(Token.Kind.AMPERSAND_AMPERSAND); return AMPERSAND_AMPERSAND; }
"|"
  { tokenSet(Token.Kind.PIPE);                return PIPE; }
"||"
  { tokenSet(Token.Kind.PIPE_PIPE);           return PIPE_PIPE; }
"~"
  { tokenSet(Token.Kind.TILDA);               return TILDA; }
"^"
  { tokenSet(Token.Kind.ACCENT);              return ACCENT; }
","
  { tokenSet(Token.Kind.COMMA);               return COMMA; }
"."
  { tokenSet(Token.Kind.DOT);                 return DOT; }
"{"
  { tokenSet(Token.Kind.BRACE_L);             return BRACE_L; }
"}"
  { tokenSet(Token.Kind.BRACE_R);             return BRACE_R; }
"["
  { tokenSet(Token.Kind.BRACKET_L);           return BRACKET_L; }
"]"
  { tokenSet(Token.Kind.BRACKET_R);           return BRACKET_R; }
"("
  { tokenSet(Token.Kind.PAREN_L);             return PAREN_L; }
")"
  { tokenSet(Token.Kind.PAREN_R);             return PAREN_R; }
"?"
  { tokenSet(Token.Kind.QUESTION);            return QUESTION; }
":"
  { tokenSet(Token.Kind.COLON);               return COLON; }
"?:"
  { tokenSet(Token.Kind.QUESTION_COLON);      return QUESTION_COLON; }
";"
  { tokenSet(Token.Kind.SEMICOLON);           return SEMICOLON; }

"null"
  { tokenSet(Token.Kind.KEYWORD_NULL);        return KEYWORD_NULL; }
"true"
  { tokenSet(Token.Kind.KEYWORD_TRUE);        return KEYWORD_TRUE; }
"false"
  { tokenSet(Token.Kind.KEYWORD_FALSE);       return KEYWORD_FALSE; }
"instanceof"
  { tokenSet(Token.Kind.KEYWORD_INSTANCEOF);  return KEYWORD_INSTANCEOF; }
"new"
  { tokenSet(Token.Kind.KEYWORD_NEW);         return KEYWORD_NEW; }
"break"
  { tokenSet(Token.Kind.KEYWORD_BREAK);       return KEYWORD_BREAK; }
"continue"
  { tokenSet(Token.Kind.KEYWORD_CONTINUE);    return KEYWORD_CONTINUE; }
"return"
  { tokenSet(Token.Kind.KEYWORD_RETURN);      return KEYWORD_RETURN; }
"if"
  { tokenSet(Token.Kind.KEYWORD_IF);          return KEYWORD_IF; }
"else"
  { tokenSet(Token.Kind.KEYWORD_ELSE);        return KEYWORD_ELSE; }
"do"
  { tokenSet(Token.Kind.KEYWORD_DO);          return KEYWORD_DO; }
"while"
  { tokenSet(Token.Kind.KEYWORD_WHILE);       return KEYWORD_WHILE; }
"for"
  { tokenSet(Token.Kind.KEYWORD_FOR);         return KEYWORD_FOR; }
"throw"
  { tokenSet(Token.Kind.KEYWORD_THROW);       return KEYWORD_THROW; }
"try"
  { tokenSet(Token.Kind.KEYWORD_TRY);         return KEYWORD_TRY; }
"catch"
  { tokenSet(Token.Kind.KEYWORD_CATCH);       return KEYWORD_CATCH; }
"finally"
  { tokenSet(Token.Kind.KEYWORD_FINALLY);     return KEYWORD_FINALLY; }
"import"
  { tokenSet(Token.Kind.KEYWORD_IMPORT);      return KEYWORD_IMPORT; }
"class"
  { tokenSet(Token.Kind.KEYWORD_CLASS);       return KEYWORD_CLASS; }
"extends"
  { tokenSet(Token.Kind.KEYWORD_EXTENDS);     return KEYWORD_EXTENDS; }
"enum"
  { tokenSet(Token.Kind.KEYWORD_ENUM);        return KEYWORD_ENUM; }
"records"
  { tokenSet(Token.Kind.KEYWORD_RECORDS);     return KEYWORD_RECORDS; }
"operator"
  { tokenSet(Token.Kind.KEYWORD_OPERATOR);    return KEYWORD_OPERATOR; }
"this"
  { tokenSet(Token.Kind.KEYWORD_THIS);        return KEYWORD_THIS; }
"super"
  { tokenSet(Token.Kind.KEYWORD_SUPER);       return KEYWORD_SUPER; }
"container"
  { tokenSet(Token.Kind.KEYWORD_CONTAINER);   return KEYWORD_CONTAINER; }
"public"
  { tokenSet(Token.Kind.KEYWORD_PUBLIC);      return KEYWORD_PUBLIC; }
"protected"
  { tokenSet(Token.Kind.KEYWORD_PROTECTED);   return KEYWORD_PROTECTED; }
"private"
  { tokenSet(Token.Kind.KEYWORD_PRIVATE);     return KEYWORD_PRIVATE; }
"static"
  { tokenSet(Token.Kind.KEYWORD_STATIC);      return KEYWORD_STATIC; }
"final"
  { tokenSet(Token.Kind.KEYWORD_FINAL);       return KEYWORD_FINAL; }
"virtual"
  { tokenSet(Token.Kind.KEYWORD_VIRTUAL);     return KEYWORD_VIRTUAL; }
"auto"
  { tokenSet(Token.Kind.KEYWORD_AUTO);        return KEYWORD_AUTO; }

0x[0-9A-Fa-f]+|[0-9]+
  {
    tokenSet(Token.Kind.INTEGER);
    lval = Long.decode(yytext());
    return INTEGER;
  }

([0-9]+\.?)|([0-9]*\.[0-9]+)([eE][-+]?[0-9]+)?
  {
    tokenSet(Token.Kind.DECIMAL);
    lval = Double.parseDouble(yytext());
    return DECIMAL;
  }

\"
  {
    string.setLength(0);
    tokenSet(Token.Kind.STRING);
    yybegin(X_STRING);
  }
<X_STRING>[^\\\"]+
  { string.append(yytext()); }
<X_STRING><<EOF>>
  {
    yyerror(new Parser.Location(tokenId, tokenId), "EOF в STRING");
    return YYEOF;
  }
<X_STRING>\\\\
  { string.append("\\\\"); }
<X_STRING>\\\"
  { string.append('"'); }
<X_STRING>\\b
  { string.append('\b'); }
<X_STRING>\\f
  { string.append('\f'); }
<X_STRING>\\n
  { string.append('\n'); }
<X_STRING>\\r
  { string.append('\r'); }
<X_STRING>\\t
  { string.append('\t'); }
<X_STRING>\"
  {
    lval = stringSet(string.toString());
    yybegin(YYINITIAL);
    return STRING;
  }
<X_STRING>[^]
  {
    yyerror(new Parser.Location(tokenId, tokenId), "недопустимый символ: '" + yytext() + "'");
    return YYUNDEF;
  }

"'"
  {
    string.setLength(0);
    tokenSet(Token.Kind.SPECIAL);
    yybegin(X_SPECIAL);
  }
<X_SPECIAL>[^\n\r\']+
  { string.append(yytext()); }
<X_SPECIAL><<EOF>>
  {
    yyerror(new Parser.Location(tokenId, tokenId), "EOF в SPECIAL");
    return YYEOF;
  }
<X_SPECIAL>"'"
  {
    lval = stringSet(string.toString());
    yybegin(YYINITIAL);
    return SPECIAL;
  }
<X_SPECIAL>[^]
  {
    yyerror(new Parser.Location(tokenId, tokenId), "недопустимый символ: '" + yytext() + "'");
    return YYUNDEF;
  }

[:jletter:][:jletterdigit:]*
  {
    tokenSet(Token.Kind.IDENTIFIER);
    lval = stringSet(yytext());
    return IDENTIFIER;
  }

"//" [^\n\r]*
  { tokenSet(Token.Kind.COMMENT); }

"/*"
  {
    commentBlockLevel++;
    tokenSet(Token.Kind.COMMENT_BLOCK);
    yybegin(X_COMMENT_BLOCK);
  }
<X_COMMENT_BLOCK>"/*"
  { commentBlockLevel++; }
<X_COMMENT_BLOCK>"/" [^*]
  {}
<X_COMMENT_BLOCK>[^/*]+
  {}
<X_COMMENT_BLOCK>\*+ [^/]
  {}
<X_COMMENT_BLOCK>\*+ "/"
  {
    commentBlockLevel--;
    if (commentBlockLevel == 0)
      yybegin(YYINITIAL);
  }

\n | \r | \r\n
  { tokenSet(Token.Kind.NEW_LINE); }

[ \t\f]+
  { tokenSet(Token.Kind.SPACE); }

<<EOF>>
  { tokenSet(Token.Kind.EOF);                 return 0; }

[^]
  {
    yyerror(new Parser.Location(tokenId, tokenId), "недопустимый символ: '" + yytext() + "'");
    return YYUNDEF;
  }
