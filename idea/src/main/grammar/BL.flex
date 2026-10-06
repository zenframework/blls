package org.zenframework.z8.blls.idea;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static org.zenframework.z8.blls.idea.psi.BlTypes.*;

%%

%class BlFlexLexer
%implements FlexLexer
%function advance
%type IElementType

NEW_LINE = \n | \r | \r\n

INTEGER = 0x[0-9A-Fa-f]+ | [0-9]+
DECIMAL = ([0-9]+\.)?[0-9]+([eE][-+]?[0-9]+)?

string_char = "\\" [\"bfnrt]?
            | [^\\\"]

special_char = [^\n\r']

%state X_STRING
%state X_SPECIAL
%state X_COMMENT
%state X_COMMENT_BLOCK
%state X_COMMENT_DOC
%state X_BAD

%%

<YYINITIAL>        "="                                   { return EQUAL; }
<YYINITIAL>        "+"                                   { return PLUS; }
<YYINITIAL>        "+="                                  { return PLUS_EQUAL; }
<YYINITIAL>        "-"                                   { return MINUS; }
<YYINITIAL>        "-="                                  { return MINUS_EQUAL; }
<YYINITIAL>        "*"                                   { return ASTERISK; }
<YYINITIAL>        "*="                                  { return ASTERISK_EQUAL; }
<YYINITIAL>        "/"                                   { return SLASH; }
<YYINITIAL>        "/="                                  { return SLASH_EQUAL; }
<YYINITIAL>        "%"                                   { return PERCENT; }
<YYINITIAL>        "%="                                  { return PERCENT_EQUAL; }
<YYINITIAL>        "=="                                  { return EQUAL_EQUAL; }
<YYINITIAL>        "!="                                  { return EXCLAMATION_EQUAL; }
<YYINITIAL>        ">"                                   { return ARROW_R; }
<YYINITIAL>        ">="                                  { return ARROW_R_EQUAL; }
<YYINITIAL>        "<"                                   { return ARROW_L; }
<YYINITIAL>        "<="                                  { return ARROW_L_EQUAL; }
<YYINITIAL>        "!"                                   { return EXCLAMATION; }
<YYINITIAL>        "&"                                   { return AMPERSAND; }
<YYINITIAL>        "&&"                                  { return AMPERSAND_AMPERSAND; }
<YYINITIAL>        "|"                                   { return PIPE; }
<YYINITIAL>        "||"                                  { return PIPE_PIPE; }
<YYINITIAL>        "~"                                   { return TILDA; }
<YYINITIAL>        "^"                                   { return ACCENT; }
<YYINITIAL>        ","                                   { return COMMA; }
<YYINITIAL>        "."                                   { return DOT; }
<YYINITIAL>        "{"                                   { return BRACE_L; }
<YYINITIAL>        "}"                                   { return BRACE_R; }
<YYINITIAL>        "["                                   { return BRACKET_L; }
<YYINITIAL>        "]"                                   { return BRACKET_R; }
<YYINITIAL>        "("                                   { return PAREN_L; }
<YYINITIAL>        ")"                                   { return PAREN_R; }
<YYINITIAL>        "?"                                   { return QUESTION; }
<YYINITIAL>        ":"                                   { return COLON; }
<YYINITIAL>        "?:"                                  { return QUESTION_COLON; }
<YYINITIAL>        ";"                                   { return SEMICOLON; }

<YYINITIAL>        "null"                                { return KEYWORD_NULL; }
<YYINITIAL>        "true"                                { return KEYWORD_TRUE; }
<YYINITIAL>        "false"                               { return KEYWORD_FALSE; }
<YYINITIAL>        "instanceof"                          { return KEYWORD_INSTANCEOF; }
<YYINITIAL>        "new"                                 { return KEYWORD_NEW; }
<YYINITIAL>        "break"                               { return KEYWORD_BREAK; }
<YYINITIAL>        "continue"                            { return KEYWORD_CONTINUE; }
<YYINITIAL>        "return"                              { return KEYWORD_RETURN; }
<YYINITIAL>        "if"                                  { return KEYWORD_IF; }
<YYINITIAL>        "else"                                { return KEYWORD_ELSE; }
<YYINITIAL>        "do"                                  { return KEYWORD_DO; }
<YYINITIAL>        "while"                               { return KEYWORD_WHILE; }
<YYINITIAL>        "for"                                 { return KEYWORD_FOR; }
<YYINITIAL>        "throw"                               { return KEYWORD_THROW; }
<YYINITIAL>        "try"                                 { return KEYWORD_TRY; }
<YYINITIAL>        "catch"                               { return KEYWORD_CATCH; }
<YYINITIAL>        "finally"                             { return KEYWORD_FINALLY; }
<YYINITIAL>        "import"                              { return KEYWORD_IMPORT; }
<YYINITIAL>        "class"                               { return KEYWORD_CLASS; }
<YYINITIAL>        "extends"                             { return KEYWORD_EXTENDS; }
<YYINITIAL>        "enum"                                { return KEYWORD_ENUM; }
<YYINITIAL>        "records"                             { return KEYWORD_RECORDS; }
<YYINITIAL>        "operator"                            { return KEYWORD_OPERATOR; }
<YYINITIAL>        "this"                                { return KEYWORD_THIS; }
<YYINITIAL>        "super"                               { return KEYWORD_SUPER; }
<YYINITIAL>        "container"                           { return KEYWORD_CONTAINER; }
<YYINITIAL>        "public"                              { return KEYWORD_PUBLIC; }
<YYINITIAL>        "protected"                           { return KEYWORD_PROTECTED; }
<YYINITIAL>        "private"                             { return KEYWORD_PRIVATE; }
<YYINITIAL>        "virtual"                             { return KEYWORD_VIRTUAL; }
<YYINITIAL>        "final"                               { return KEYWORD_FINAL; }
<YYINITIAL>        "static"                              { return KEYWORD_STATIC; }
<YYINITIAL>        "auto"                                { return KEYWORD_AUTO; }

<YYINITIAL>        "//"                                  { yypushback(2); yybegin(X_COMMENT); }
<X_COMMENT>        "//"[^\n]*                            { yybegin(YYINITIAL); return COMMENT; }

<YYINITIAL>        "/*"                                  { yypushback(2); yybegin(X_COMMENT_BLOCK); }
<X_COMMENT_BLOCK>  "*/"                                  { yybegin(YYINITIAL); return COMMENT_BLOCK; }
<X_COMMENT_BLOCK>  [^]                                   {}

<YYINITIAL>        "/**"                                 { yypushback(3); yybegin(X_COMMENT_DOC); }
<X_COMMENT_DOC>    "*/"                                  { yybegin(YYINITIAL); return COMMENT_DOC; }
<X_COMMENT_DOC>    [^]                                   {}

<YYINITIAL>        {INTEGER}                             { return INTEGER; }
<YYINITIAL>        {DECIMAL}                             { return DECIMAL; }

<YYINITIAL>        "\""                                  { yybegin(X_STRING); }
<X_STRING>         {string_char}*"\""                    { yybegin(YYINITIAL); return STRING; }
<X_STRING>         [^]                                   { yypushback(1); yybegin(X_BAD); }

<YYINITIAL>        "'"                                   { yybegin(X_SPECIAL); }
<X_SPECIAL>        {special_char}*"'"                    { yybegin(YYINITIAL); return SPECIAL; }
<X_SPECIAL>        [^]                                   { yypushback(1); yybegin(X_BAD); }

<YYINITIAL>        [:jletter:][:jletterdigit:]*          { return IDENTIFIER; }

<X_BAD>            [^\n]*{NEW_LINE}                      { yybegin(YYINITIAL); return BAD_CHARACTER; }

<YYINITIAL>        [\s]+                                 { return WHITE_SPACE; }

                   [^]                                   { return BAD_CHARACTER; }
