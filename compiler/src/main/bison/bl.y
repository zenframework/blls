%require "3.2"
%language "java"
%define api.package org.zenframework.z8.blls.compiler
%define api.parser.public
%define api.parser.abstract
%define api.parser.class {Parser}
%define api.position.type {Integer}
%define api.token.raw
%define parse.error verbose
%locations

%code imports {
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import module org.zenframework.z8.blls.ast;
}

%code {
  protected abstract Ast ast();
  protected abstract void sourceSet(Source result);
  protected abstract int nodeSet(Location location, Node.Data data);
  protected abstract void siblingSet(int nodeId, int nodeSiblingId);

  private int definitionOf(int startId, List<Part.Access> accesses, List<Part.Index> indices) {
    int nodeId = 0;
    for (var i : accesses) {
      nodeId = nodeSet(new Location(startId, i.endId()), new Definition.Reference(nodeId, i.nameId()));
    }
    for (var i : indices) {
      if (i.valueId() == 0) {
        nodeId = nodeSet(new Location(startId, i.endId()), new Definition.Array(nodeId));
      } else if (ast().data(i.valueId()) instanceof Expression.Access v && v.childId() == 0) {
        nodeId = nodeSet(
          new Location(startId, i.endId()),
          new Definition.Map(
            nodeId,
            nodeSet(
              new Location(
                ast().start(ast().node(i.valueId())).id(),
                ast().end(ast().node(i.valueId())).id()
              ),
              new Definition.Reference(0, v.nameId())
            )
          )
        );
      } else {
        // TODO: error
        System.err.println("TODO");
        return 0;
      }
    }
    return nodeId;
  }

  private int accessesOf(int startId, List<Part.Access> accesses) {
    var iter = accesses.iterator();
    int nodeId = 0;
    while (iter.hasNext()) {
      var i = iter.next();
      nodeId = nodeSet(new Location(startId, i.endId()), new Expression.Access(nodeId, i.nameId()));
    }
    return nodeId;
  }

  private int expressionOf(int startId, List<Part> parts, int childId) {
    for (var p : parts) switch (p) {
      case Part.Access v -> {
        childId = nodeSet(new Location(startId, v.endId()), new Expression.Access(childId, v.nameId()));
      }
      case Part.Index v -> {
        if (v.valueId() == 0) {
          // TODO: error
          System.err.println("TODO");
          return 0;
        }
        childId = nodeSet(new Location(startId, v.endId()), new Expression.Index(childId, v.valueId()));
      }
      case Part.Invoke v -> {
        childId = nodeSet(new Location(startId, v.endId()), new Expression.Invoke(childId, v.nameId(), v.argumentsId()));
      }
    }
    return childId;
  }

  private int castOf(int nodeParenId, int childId) {
    Node<Expression.Paren> paren = ast().node(nodeParenId);
    if (paren.data().child(ast()).data() instanceof Expression.Access v && v.childId() == 0) {
      int definitionId = nodeSet(new Location(ast().start(paren.data().child(ast())).id(), ast().end(paren.data().child(ast())).id()), new Definition.Reference(0, v.nameId()));
      return nodeSet(new Location(ast().start(paren).id(), ast().end(paren).id()), new Expression.Cast(definitionId, childId));
    } else {
      // TODO: error
      System.err.println("TODO");
      return 0;
    }
  }

  private sealed interface Part {
    record Access(int endId, int nameId) implements Part {
    }

    record Index(int endId, int valueId) implements Part {
    }

    record Invoke(int endId, int nameId, int argumentsId) implements Part {
    }
  }
}





%token EQUAL "="
%token PLUS "+"
%token PLUS_EQUAL "+="
%token MINUS "-"
%token MINUS_EQUAL "-="
%token ASTERISK "*"
%token ASTERISK_EQUAL "*="
%token SLASH "/"
%token SLASH_EQUAL "/="
%token PERCENT "%"
%token PERCENT_EQUAL "%="
%token EQUAL_EQUAL "=="
%token EXCLAMATION_EQUAL "!="
%token ARROW_R ">"
%token ARROW_R_EQUAL ">="
%token ARROW_L "<"
%token ARROW_L_EQUAL "<="
%token EXCLAMATION "!"
%token AMPERSAND "&"
%token AMPERSAND_AMPERSAND "&&"
%token PIPE "|"
%token PIPE_PIPE "||"
%token TILDA "~"
%token ACCENT "^"
%token COMMA ","
%token DOT "."
%token BRACE_L "{"
%token BRACE_R "}"
%token BRACKET_L "["
%token BRACKET_R "]"
%token PAREN_L "("
%token PAREN_R ")"
%token QUESTION "?"
%token COLON ":"
%token QUESTION_COLON "?:"
%token SEMICOLON ";"

%token KEYWORD_NULL "null"
%token KEYWORD_TRUE "true"
%token KEYWORD_FALSE "false"
%token KEYWORD_INSTANCEOF "instanceof"
%token KEYWORD_NEW "new"
%token KEYWORD_BREAK "break"
%token KEYWORD_CONTINUE "continue"
%token KEYWORD_RETURN "return"
%token KEYWORD_IF "if"
%token KEYWORD_ELSE "else"
%token KEYWORD_DO "do"
%token KEYWORD_WHILE "while"
%token KEYWORD_FOR "for"
%token KEYWORD_THROW "throw"
%token KEYWORD_TRY "try"
%token KEYWORD_CATCH "catch"
%token KEYWORD_FINALLY "finally"
%token KEYWORD_IMPORT "import"
%token KEYWORD_CLASS "class"
%token KEYWORD_EXTENDS "extends"
%token KEYWORD_ENUM "enum"
%token KEYWORD_RECORDS "records"
%token KEYWORD_OPERATOR "operator"
%token KEYWORD_THIS "this"
%token KEYWORD_SUPER "super"
%token KEYWORD_CONTAINER "container"
%token KEYWORD_PUBLIC "public"
%token KEYWORD_PROTECTED "protected"
%token KEYWORD_PRIVATE "private"
%token KEYWORD_STATIC "static"
%token KEYWORD_FINAL "final"
%token KEYWORD_VIRTUAL "virtual"
%token KEYWORD_AUTO "auto"

%token <Long> INTEGER "целое"
%token <Double> DECIMAL "число"
%token <Integer> STRING "строка"
%token <Integer> SPECIAL "данные"
%token <Integer> IDENTIFIER "название"

%left EQUAL PLUS_EQUAL MINUS_EQUAL ASTERISK_EQUAL SLASH_EQUAL PERCENT_EQUAL
%right QUESTION COLON
%left PIPE_PIPE
%left AMPERSAND_AMPERSAND
%left PIPE
%left ACCENT
%left AMPERSAND
%nonassoc EQUAL_EQUAL EXCLAMATION_EQUAL
%nonassoc ARROW_L ARROW_L_EQUAL ARROW_R ARROW_R_EQUAL
%precedence KEYWORD_INSTANCEOF
%left PLUS MINUS
%left ASTERISK SLASH PERCENT
%left QUESTION_COLON
%precedence PLUS_UNARY MINUS_UNARY EXCLAMATION TILDA

%precedence KEYWORD_IF
%precedence KEYWORD_ELSE





%type source

%type <Integer> imports
%type <SimpleEntry<Integer, Integer>> imports1
%type <Integer> import

%type <Integer> annotations
%type <SimpleEntry<Integer, Integer>> annotations1
%type <Integer> annotation

%type <Integer> parameters
%type <SimpleEntry<Integer, Integer>> parameters1
%type <Integer> parameter

%type <Integer> modifiers1 modifier
%type <Operation> operation

%type <Integer> klasses_enumerations
%type <SimpleEntry<Integer, Integer>> klasses_enumerations1
%type <Integer> klass_enumeration klass enumeration

%type <Integer> declaration_children
%type <SimpleEntry<Integer, Integer>> declaration_children1
%type <Integer> declaration_child
%type <Integer> declaration_records
%type <SimpleEntry<Integer, Integer>> declaration_records1
%type <Integer> declaration_record

%type <Integer> enumeration_children enumeration_children1
%type <SimpleEntry<Integer, Integer>> enumeration_children2
%type <Integer> enumeration_child

%type <Integer> statements
%type <SimpleEntry<Integer, Integer>> statements1
%type <Integer> statement statement_variable statement_block statement_try_catches
%type <SimpleEntry<Integer, Integer>> statement_try_catches1
%type <Integer> statement_try_catch

%type <Integer> expression expression_no_array expression17_part
%type <Integer> expression16 expression16_no_array
%type <Integer> expression15 expression15_no_array
%type <Integer> expression14 expression14_no_array
%type <Integer> expression13 expression13_no_array
%type <Integer> expression12 expression12_no_array
%type <Integer> expression11 expression11_no_array
%type <Integer> expression10 expression10_no_array
%type <Integer> expression9 expression9_no_array
%type <Integer> expression8 expression8_no_array
%type <Integer> expression7 expression7_no_array
%type <Integer> expression6 expression6_no_array
%type <Integer> expression5 expression5_no_array expression5_part
%type <Integer> expression4 expression4_no_array expression4_part
%type <Integer> expression3
%type <Integer> expression2
%type <Integer> expression_array_children
%type <SimpleEntry<Integer, Integer>> expression_array_children1
%type <Integer> expression_array_child
%type <Integer> expression_containers1
%type <List<Part>> expression_part expression_access_part expression_index_part expression_invoke_part
%type <Integer> expression1
%type <Integer> expression_paren
%type <Integer> expression_special
%type <List<Part.Index>> indices indices1
%type <Part.Index> index
%type <List<Part.Invoke>> invokes1
%type <Part.Invoke> invoke
%type <Integer> expression_invoke_arguments
%type <SimpleEntry<Integer, Integer>> expression_invoke_arguments1
%type <Integer> expression_invoke_argument
%type <List<Part.Access>> identifiers


%%


source
: imports klasses_enumerations
  { sourceSet(new Source($1, $2)); }
;

imports
: %empty
  { $$ = 0; }
| imports1
  { $$ = $1.getKey(); }
;

imports1
: import
  { $$ = new SimpleEntry<>($1, $1); }
| imports1 import
  { siblingSet($1.getValue(), $2); $1.setValue($2); }
;

import
: KEYWORD_IMPORT identifiers SEMICOLON
  { $$ = nodeSet(@$, new Import(definitionOf(@2.begin, $2, List.of()))); }
;

annotations
: %empty
  { $$ = 0; }
| annotations1
  { $$ = $1.getKey(); }
;

annotations1
: annotation
  { $$ = new SimpleEntry<>($1, $1); }
| annotations1 annotation
  { siblingSet($1.getValue(), $2); $1.setValue($2); }
;

annotation
: BRACKET_L IDENTIFIER BRACKET_R
  { $$ = nodeSet(@$, new Annotation($2, 0)); }
| BRACKET_L IDENTIFIER expression BRACKET_R
  { $$ = nodeSet(@$, new Annotation($2, $3)); }
;

parameters
: %empty
  { $$ = 0; }
| parameters1
  { $$ = $1.getKey(); }
;

parameters1
: parameter
  { $$ = new SimpleEntry<>($1, $1); }
| parameters1 COMMA parameter
  { siblingSet($1.getValue(), $3); $1.setValue($3); }
;

parameter
: identifiers indices IDENTIFIER
  { $$ = nodeSet(@$, new Parameter(definitionOf(@1.begin, $1, $2), $3)); }
;

modifiers1
: modifier
| modifiers1 modifier
  { $$ = $1 | $2; }
;

modifier
: KEYWORD_PUBLIC
  { $$ = Declaration.PUBLIC; }
| KEYWORD_PROTECTED
  { $$ = Declaration.PROTECTED; }
| KEYWORD_PRIVATE
  { $$ = Declaration.PRIVATE; }
| KEYWORD_STATIC
  { $$ = Declaration.STATIC; }
| KEYWORD_FINAL
  { $$ = Declaration.FINAL; }
| KEYWORD_VIRTUAL
  { $$ = Declaration.VIRTUAL; }
| KEYWORD_AUTO
  { $$ = Declaration.AUTO; }
;

operation
: ASTERISK
  { $$ = Operation.MULTIPLY; }
| SLASH
  { $$ = Operation.DIVIDE; }
| PERCENT
  { $$ = Operation.MODULE; }
| PLUS
  { $$ = Operation.ADD; }
| MINUS
  { $$ = Operation.SUBTRACT; }
| ARROW_L
  { $$ = Operation.LESS; }
| ARROW_L_EQUAL
  { $$ = Operation.LESS_EQUAL; }
| ARROW_R
  { $$ = Operation.MORE; }
| ARROW_R_EQUAL
  { $$ = Operation.MORE_EQUAL; }
| EQUAL_EQUAL
  { $$ = Operation.EQUAL; }
| EXCLAMATION_EQUAL
  { $$ = Operation.NOT_EQUAL; }
| AMPERSAND
  { $$ = Operation.BIT_AND; }
| AMPERSAND_AMPERSAND
  { $$ = Operation.AND; }
| PIPE
  { $$ = Operation.BIT_OR; }
| PIPE_PIPE
  { $$ = Operation.OR; }
| TILDA
  { $$ = Operation.BIT_NOT; }
| ACCENT
  { $$ = Operation.BIT_XOR; }
| EQUAL
  { $$ = Operation.ASSIGN; }
| EXCLAMATION
  { $$ = Operation.NOT; }
;

klasses_enumerations
: %empty
  { $$ = 0; }
| klasses_enumerations1
  { $$ = $1.getKey(); }
;

klasses_enumerations1
: klass_enumeration
  { $$ = new SimpleEntry<>($1, $1); }
| klasses_enumerations1 klass_enumeration
  { siblingSet($1.getValue(), $2); $1.setValue($2); }
;

klass_enumeration
: klass
| enumeration
;

klass
: annotations KEYWORD_CLASS IDENTIFIER BRACE_L declaration_children BRACE_R
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Klass($1, 0, $3, 0, nodeSet(new Location(@4.begin, @6.end), new Declaration($5)))); }
| annotations modifiers1 KEYWORD_CLASS IDENTIFIER BRACE_L declaration_children BRACE_R
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Klass($1, $2, $4, 0, nodeSet(new Location(@5.begin, @7.end), new Declaration($6)))); }
| annotations KEYWORD_CLASS IDENTIFIER KEYWORD_EXTENDS identifiers BRACE_L declaration_children BRACE_R
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Klass($1, 0, $3, definitionOf(@5.begin, $5, List.of()), nodeSet(new Location(@6.begin, @8.end), new Declaration($7)))); }
| annotations modifiers1 KEYWORD_CLASS IDENTIFIER KEYWORD_EXTENDS identifiers BRACE_L declaration_children BRACE_R
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Klass($1, $2, $4, definitionOf(@6.begin, $6, List.of()), nodeSet(new Location(@7.begin, @9.end), new Declaration($8)))); }
;

declaration_children
: %empty
  { $$ = 0; }
| declaration_children1
  { $$ = $1.getKey(); }
;

declaration_children1
: declaration_child
  { $$ = new SimpleEntry<>($1, $1); }
| declaration_children1 declaration_child
  { siblingSet($1.getValue(), $2); $1.setValue($2); }
;

declaration_child
: annotations identifiers indices IDENTIFIER SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Field($1, 0, definitionOf(@2.begin, $2, $3), $4, 0)); }
| annotations modifiers1 identifiers indices IDENTIFIER SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Field($1, $2, definitionOf(@3.begin, $3, $4), $5, 0)); }
| annotations identifiers indices IDENTIFIER EQUAL expression SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Field($1, 0, definitionOf(@2.begin, $2, $3), $4, nodeSet(@6, new Statement.Expression($6)))); }
| annotations modifiers1 identifiers indices IDENTIFIER EQUAL expression SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Field($1, $2, definitionOf(@3.begin, $3, $4), $5, nodeSet(@7, new Statement.Expression($7)))); }
| annotations identifiers SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Initialize($1, nodeSet(@2, new Statement.Expression(accessesOf(@2.begin, $2))))); }
| annotations identifiers EQUAL expression SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Initialize($1, nodeSet(new Location(@2.begin, @4.end), new Statement.Expression(nodeSet(new Location(@2.begin, @4.end), new Expression.Binary(accessesOf(@2.begin, $2), Operation.ASSIGN, $4)))))); }
| annotations identifiers PLUS_EQUAL expression SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Initialize($1, nodeSet(new Location(@2.begin, @4.end), new Statement.Expression(nodeSet(new Location(@2.begin, @4.end), new Expression.Binary(accessesOf(@2.begin, $2), Operation.ASSIGN_ADD, $4)))))); }
| annotations identifiers indices IDENTIFIER PAREN_L parameters PAREN_R SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Method($1, 0, definitionOf(@2.begin, $2, $3), $4, $6, 0)); }
| annotations modifiers1 identifiers indices IDENTIFIER PAREN_L parameters PAREN_R SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Method($1, $2, definitionOf(@3.begin, $3, $4), $5, $7, 0)); }
| annotations identifiers indices IDENTIFIER PAREN_L parameters PAREN_R statement_block
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Method($1, 0, definitionOf(@2.begin, $2, $3), $4, $6, $8)); }
| annotations modifiers1 identifiers indices IDENTIFIER PAREN_L parameters PAREN_R statement_block
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Method($1, $2, definitionOf(@3.begin, $3, $4), $5, $7, $9)); }
| annotations KEYWORD_OPERATOR PAREN_L parameters PAREN_R SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Priority($1, 0, $4, 0)); }
| annotations modifiers1 KEYWORD_OPERATOR PAREN_L parameters PAREN_R SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Priority($1, $2, $5, 0)); }
| annotations KEYWORD_OPERATOR PAREN_L parameters PAREN_R statement_block
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Priority($1, 0, $4, $6)); }
| annotations modifiers1 KEYWORD_OPERATOR PAREN_L parameters PAREN_R statement_block
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Priority($1, $2, $5, $7)); }
| annotations KEYWORD_OPERATOR IDENTIFIER PAREN_L parameters PAREN_R SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Cast($1, 0, nodeSet(@3, new Definition.Reference(0, $3)), $5, 0)); }
| annotations modifiers1 KEYWORD_OPERATOR IDENTIFIER PAREN_L parameters PAREN_R SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Cast($1, $2, nodeSet(@4, new Definition.Reference(0, $4)), $6, 0)); }
| annotations KEYWORD_OPERATOR IDENTIFIER PAREN_L parameters PAREN_R statement_block
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Cast($1, 0, nodeSet(@3, new Definition.Reference(0, $3)), $5, $7)); }
| annotations modifiers1 KEYWORD_OPERATOR IDENTIFIER PAREN_L parameters PAREN_R statement_block
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Cast($1, $2, nodeSet(@4, new Definition.Reference(0, $4)), $6, $8)); }
| annotations identifiers indices KEYWORD_OPERATOR operation PAREN_L parameters PAREN_R SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Operator($1, 0, definitionOf(@2.begin, $2, $3), $5, $7, 0)); }
| annotations modifiers1 identifiers indices KEYWORD_OPERATOR operation PAREN_L parameters PAREN_R SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Operator($1, $2, definitionOf(@3.begin, $3, $4), $6, $8, 0)); }
| annotations identifiers indices KEYWORD_OPERATOR operation PAREN_L parameters PAREN_R statement_block
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Operator($1, 0, definitionOf(@2.begin, $2, $3), $5, $7, $9)); }
| annotations modifiers1 identifiers indices KEYWORD_OPERATOR operation PAREN_L parameters PAREN_R statement_block
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Operator($1, $2, definitionOf(@3.begin, $3, $4), $6, $8, $10)); }
| KEYWORD_RECORDS BRACE_L declaration_records BRACE_R
  { $$ = nodeSet(@$, new Declaration.Records($3)); }
;

declaration_records
: %empty
  { $$ = 0; }
| declaration_records1
  { $$ = $1.getKey(); }
;

declaration_records1
: declaration_record
  { $$ = new SimpleEntry<>($1, $1); }
| declaration_records1 declaration_record
  { siblingSet($1.getValue(), $2); $1.setValue($2); }
;

declaration_record
: annotations IDENTIFIER EQUAL expression_special SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Records.Child($1, 0, $2, $4)); }
| annotations modifiers1 IDENTIFIER EQUAL expression_special SEMICOLON
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Declaration.Records.Child($1, $2, $3, $5)); }
;

enumeration
: annotations KEYWORD_ENUM IDENTIFIER BRACE_L enumeration_children BRACE_R
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Enumeration($1, 0, $3, $5)); }
| annotations modifiers1 KEYWORD_ENUM IDENTIFIER BRACE_L enumeration_children BRACE_R
  { $$ = nodeSet(new Location($1 == 0 ? @2.begin : @1.begin, @$.end), new Enumeration($1, $2, $4, $6)); }
;

enumeration_children
: %empty
  { $$ = 0; }
| enumeration_children1
;

enumeration_children1
: enumeration_children2
  { $$ = $1.getKey(); }
| enumeration_children2 COMMA
  { $$ = $1.getKey(); }
;

enumeration_children2
: enumeration_child
  { $$ = new SimpleEntry<>($1, $1); }
| enumeration_children2 COMMA enumeration_child
  { siblingSet($1.getValue(), $3); $1.setValue($3); }
;

enumeration_child
: IDENTIFIER
  { $$ = nodeSet(@$, new Enumeration.Child($1)); }
;





statements
: %empty
  { $$ = 0; }
| statements1
  { $$ = $1.getKey(); }
;

statements1
: statement
  { $$ = new SimpleEntry<>($1, $1); }
| statements1 statement
  { siblingSet($1.getValue(), $2); $1.setValue($2); }
;

statement
: statement_block
| expression_no_array SEMICOLON
  { $$ = nodeSet(@$, new Statement.Expression($1)); }
| identifiers indices IDENTIFIER SEMICOLON
  { $$ = nodeSet(@$, new Statement.Variable(definitionOf(@1.begin, $1, $2), $3, 0)); }
| statement_variable
| KEYWORD_RETURN SEMICOLON
  { $$ = nodeSet(@$, new Statement.Return(0)); }
| KEYWORD_RETURN expression SEMICOLON
  { $$ = nodeSet(@$, new Statement.Return($2)); }
| KEYWORD_BREAK SEMICOLON
  { $$ = nodeSet(@$, new Statement.Break()); }
| KEYWORD_CONTINUE SEMICOLON
  { $$ = nodeSet(@$, new Statement.Continue()); }
| KEYWORD_THROW expression SEMICOLON
  { $$ = nodeSet(@$, new Statement.Throw($2)); }
| KEYWORD_TRY statement_block statement_try_catches
  { $$ = nodeSet(new Location(@1.begin, $3 == 0 ? @2.end : @3.end), new Statement.Try($2, $3, 0)); }
| KEYWORD_TRY statement_block statement_try_catches KEYWORD_FINALLY statement_block
  { $$ = nodeSet(@$, new Statement.Try($2, $3, $5)); }
| KEYWORD_IF expression_paren statement
  { $$ = nodeSet(@$, new Statement.Condition($2, $3, 0)); }
| KEYWORD_IF expression_paren statement KEYWORD_ELSE statement
  { $$ = nodeSet(@$, new Statement.Condition($2, $3, $5)); }
| KEYWORD_DO statement KEYWORD_WHILE expression_paren SEMICOLON
  { $$ = nodeSet(@$, new Statement.LoopDo($2, $4)); }
| KEYWORD_WHILE expression_paren statement
  { $$ = nodeSet(@$, new Statement.LoopWhile($2, $3)); }
| KEYWORD_FOR PAREN_L SEMICOLON expression SEMICOLON expression PAREN_R statement
  { $$ = nodeSet(@$, new Statement.LoopFor(0, $4, $6, $8)); }
| KEYWORD_FOR PAREN_L statement_variable expression SEMICOLON expression PAREN_R statement
  { $$ = nodeSet(@$, new Statement.LoopFor($3, $4, $6, $8)); }
| KEYWORD_FOR PAREN_L expression SEMICOLON expression SEMICOLON expression PAREN_R statement
  { $$ = nodeSet(@$, new Statement.LoopFor($3, $5, $7, $9)); }
| KEYWORD_FOR PAREN_L identifiers indices IDENTIFIER COLON expression PAREN_R statement
  { $$ = nodeSet(@$, new Statement.LoopForEach(definitionOf(@3.begin, $3, $4), $5, $7, $9)); }
;

statement_block
: BRACE_L statements BRACE_R
  { $$ = nodeSet(@$, new Statement.Block($2)); }
;

statement_variable
: identifiers indices IDENTIFIER EQUAL expression SEMICOLON
  { $$ = nodeSet(@$, new Statement.Variable(definitionOf(@1.begin, $1, $2), $3, $5)); }
;

statement_try_catches
: %empty
  { $$ = 0; }
| statement_try_catches1
  { $$ = $1.getKey(); }
;

statement_try_catches1
: statement_try_catch
  { $$ = new SimpleEntry<>($1, $1); }
| statement_try_catches1 statement_try_catch
  { siblingSet($1.getValue(), $2); $1.setValue($2); }
;

statement_try_catch
: KEYWORD_CATCH PAREN_L identifiers indices IDENTIFIER PAREN_R statement_block
  { $$ = nodeSet(@$, new Statement.Try.Catch(definitionOf(@3.begin, $3, $4), $5, $7)); }
;





expression
: expression16
| expression17_part
;

expression_no_array
: expression16_no_array
| expression17_part
;

expression17_part
: expression2 EQUAL expression
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ASSIGN, $3)); }
| expression2 PLUS_EQUAL expression
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ASSIGN_ADD, $3)); }
| expression2 MINUS_EQUAL expression
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ASSIGN_SUBTRACT, $3)); }
| expression2 ASTERISK_EQUAL expression
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ASSIGN_MULTIPLY, $3)); }
| expression2 SLASH_EQUAL expression
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ASSIGN_DIVIDE, $3)); }
| expression2 PERCENT_EQUAL expression
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ASSIGN_MODULE, $3)); }
;

expression16
: expression15
| expression15 QUESTION expression16 COLON expression16
  { $$ = nodeSet(@$, new Expression.Ternary($1, $3, $5)); }
;

expression16_no_array
: expression15_no_array
| expression15_no_array QUESTION expression16 COLON expression16
  { $$ = nodeSet(@$, new Expression.Ternary($1, $3, $5)); }
;

expression15
: expression14
| expression14 PIPE_PIPE expression15
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.OR, $3)); }
;

expression15_no_array
: expression14_no_array
| expression14_no_array PIPE_PIPE expression15
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.OR, $3)); }
;

expression14
: expression13
| expression13 AMPERSAND_AMPERSAND expression14
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.AND, $3)); }
;

expression14_no_array
: expression13_no_array
| expression13_no_array AMPERSAND_AMPERSAND expression14
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.AND, $3)); }
;

expression13
: expression12
| expression12 PIPE expression13
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.BIT_OR, $3)); }
;

expression13_no_array
: expression12_no_array
| expression12_no_array PIPE expression13
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.BIT_OR, $3)); }
;

expression12
: expression11
| expression11 ACCENT expression12
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.BIT_XOR, $3)); }
;

expression12_no_array
: expression11_no_array
| expression11_no_array ACCENT expression12
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.BIT_XOR, $3)); }
;

expression11
: expression10
| expression10 AMPERSAND expression11
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.BIT_AND, $3)); }
;

expression11_no_array
: expression10_no_array
| expression10_no_array AMPERSAND expression11
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.BIT_AND, $3)); }
;

expression10
: expression9
| expression9 EQUAL_EQUAL expression10
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.EQUAL, $3)); }
| expression9 EXCLAMATION_EQUAL expression10
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.NOT_EQUAL, $3)); }
;

expression10_no_array
: expression9_no_array
| expression9_no_array EQUAL_EQUAL expression10
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.EQUAL, $3)); }
| expression9_no_array EXCLAMATION_EQUAL expression10
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.NOT_EQUAL, $3)); }
;

expression9
: expression8
| expression8 ARROW_L expression9
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.LESS, $3)); }
| expression8 ARROW_L_EQUAL expression9
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.LESS_EQUAL, $3)); }
| expression8 ARROW_R expression9
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.MORE, $3)); }
| expression8 ARROW_R_EQUAL expression9
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.MORE_EQUAL, $3)); }
| expression8 KEYWORD_INSTANCEOF identifiers indices
  { $$ = nodeSet(@$, new Expression.Instanceof($1, definitionOf(@3.begin, $3, $4))); }
;

expression9_no_array
: expression8_no_array
| expression8_no_array ARROW_L expression9
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.LESS, $3)); }
| expression8_no_array ARROW_L_EQUAL expression9
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.LESS_EQUAL, $3)); }
| expression8_no_array ARROW_R expression9
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.MORE, $3)); }
| expression8_no_array ARROW_R_EQUAL expression9
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.MORE_EQUAL, $3)); }
| expression8_no_array KEYWORD_INSTANCEOF identifiers indices
  { $$ = nodeSet(@$, new Expression.Instanceof($1, definitionOf(@3.begin, $3, $4))); }
;

expression8
: expression7
| expression7 PLUS expression8
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ADD, $3)); }
| expression7 MINUS expression8
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.SUBTRACT, $3)); }
;

expression8_no_array
: expression7_no_array
| expression7_no_array PLUS expression8
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ADD, $3)); }
| expression7_no_array MINUS expression8
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.SUBTRACT, $3)); }
;

expression7
: expression6
| expression6 ASTERISK expression7
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.MULTIPLY, $3)); }
| expression6 SLASH expression7
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.DIVIDE, $3)); }
| expression6 PERCENT expression7
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.MODULE, $3)); }
;

expression7_no_array
: expression6_no_array
| expression6_no_array ASTERISK expression7
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.MULTIPLY, $3)); }
| expression6_no_array SLASH expression7
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.DIVIDE, $3)); }
| expression6_no_array PERCENT expression7
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.MODULE, $3)); }
;

expression6
: expression5
| expression5 QUESTION_COLON expression6
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ELVIS, $3)); }
;

expression6_no_array
: expression5_no_array
| expression5_no_array QUESTION_COLON expression6
  { $$ = nodeSet(@$, new Expression.Binary($1, Operation.ELVIS, $3)); }
;

expression5
: expression4
| expression5_part
;

expression5_no_array
: expression4_no_array
| expression5_part
;

expression5_part
: PLUS expression5
  { $$ = nodeSet(@$, new Expression.Unary(Operation.PLUS, $2)); }
| MINUS expression5
  { $$ = nodeSet(@$, new Expression.Unary(Operation.NEGATE, $2)); }
| EXCLAMATION expression5
  { $$ = nodeSet(@$, new Expression.Unary(Operation.NOT, $2)); }
| TILDA expression5
  { $$ = nodeSet(@$, new Expression.Unary(Operation.BIT_NOT, $2)); }
;

expression4
: expression3
| expression4_part
;

expression4_no_array
: expression2
| expression4_part
;

expression4_part
: expression_paren expression4
  {
    int c = castOf($1, $2);
    if (c == 0) return YYERROR;
    $$ = c;
  }
;

expression3
: expression2
| BRACE_L expression_array_children BRACE_R
  { $$ = nodeSet(@$, new Expression.Array(0, $2)); }
;

expression2
: expression1
| expression1 expression_part
  {
    int e = expressionOf(@$.begin, $2, $1);
    if (e == 0) return YYERROR;
    $$ = e;
  }
| KEYWORD_SUPER expression_invoke_part
  {
    int e = expressionOf(@$.begin, $2, nodeSet(@1, new Expression.Super()));
    if (e == 0) return YYERROR;
    $$ = e;
  }
| expression_containers1
| expression_containers1 expression_access_part
  {
    int e = expressionOf(@$.begin, $2, $1);
    if (e == 0) return YYERROR;
    $$ = e;
  }
| expression_containers1 expression_invoke_part
  {
    int e = expressionOf(@$.begin, $2, $1);
    if (e == 0) return YYERROR;
    $$ = e;
  }
| identifiers
  { $$ = accessesOf(@1.begin, $1); }
| identifiers expression_index_part
  {
    int e = expressionOf(@$.begin, $2, accessesOf(@1.begin, $1));
    if (e == 0) return YYERROR;
    $$ = e;
  }
| identifiers expression_invoke_part
  {
    int e = expressionOf(@$.begin, $2, accessesOf(@1.begin, $1));
    if (e == 0) return YYERROR;
    $$ = e;
  }
;

expression_array_children
: %empty
  { $$ = 0; }
| expression_array_children1
  { $$ = $1.getKey(); }
| expression_array_children1 COMMA
  { $$ = $1.getKey(); }
;

expression_array_children1
: expression_array_child
  { $$ = new SimpleEntry<>($1, $1); }
| expression_array_children1 COMMA expression_array_child
  { siblingSet($1.getValue(), $3); $1.setValue($3); }
;

expression_array_child
: expression
  { $$ = nodeSet(@$, new Expression.Array.Child.Value($1)); }
| PAREN_L expression COMMA expression PAREN_R
  { $$ = nodeSet(@$, new Expression.Array.Child.Entry($2, $4)); }
;

expression_containers1
: KEYWORD_CONTAINER
  { $$ = nodeSet(@$, new Expression.Container(0)); }
| expression_containers1 DOT KEYWORD_CONTAINER
  { $$ = nodeSet(@$, new Expression.Container($1)); }
;

expression_part
: expression_access_part
| expression_index_part
| expression_invoke_part
;

expression_access_part
: DOT identifiers
  { $$ = $2; }
| DOT identifiers expression_index_part
  { $3.addAll(0, $2); $$ = $3; }
| DOT identifiers expression_invoke_part
  { $3.addAll(0, $2); $$ = $3; }
;

expression_index_part
: indices1
  { $$ = $1; }
| indices1 expression_access_part
  { $2.addAll(0, $1); $$ = $2; }
| indices1 expression_invoke_part
  { $2.addAll(0, $1); $$ = $2; }
;

expression_invoke_part
: invokes1
  { $$ = $1; }
| invokes1 expression_access_part
  { $2.addAll(0, $1); $$ = $2; }
| invokes1 expression_index_part
  { $2.addAll(0, $1); $$ = $2; }
;

expression1
: expression_paren
| KEYWORD_NULL
  { $$ = nodeSet(@$, new Expression.Null()); }
| KEYWORD_TRUE
  { $$ = nodeSet(@$, new Expression.Boolean(true)); }
| KEYWORD_FALSE
  { $$ = nodeSet(@$, new Expression.Boolean(false)); }
| INTEGER
  { $$ = nodeSet(@$, new Expression.Integer($1)); }
| DECIMAL
  { $$ = nodeSet(@$, new Expression.Decimal($1)); }
| STRING
  { $$ = nodeSet(@$, new Expression.String($1)); }
| expression_special
| KEYWORD_THIS
  { $$ = nodeSet(@$, new Expression.This()); }
| KEYWORD_NEW IDENTIFIER
  { $$ = nodeSet(@$, new Expression.New(nodeSet(@2, new Definition.Reference(0, $2)))); }
| identifiers indices BRACE_L expression_array_children BRACE_R
  { $$ = nodeSet(@$, new Expression.Array(definitionOf(@1.begin, $1, $2), $4)); }
| IDENTIFIER PAREN_L expression_invoke_arguments PAREN_R
  { $$ = nodeSet(@$, new Expression.Invoke(0, $1, $3)); }
| KEYWORD_CLASS BRACE_L declaration_children BRACE_R
  { $$ = nodeSet(@$, new Expression.Object(nodeSet(new Location(@2.begin, @4.end), new Declaration($3)))); }
;

expression_paren
: PAREN_L expression PAREN_R
  { $$ = nodeSet(@$, new Expression.Paren($2)); }
;

expression_special
: SPECIAL
  { $$ = nodeSet(@$, new Expression.Special($1)); }
;

indices
: %empty
  { $$ = new ArrayList<>(); }
| indices1
;

indices1
: index
  { $$ = new ArrayList<>(List.of($1)); }
| indices1 index
  { $1.add($2); $$ = $1; }
;

index
: BRACKET_L BRACKET_R
  { $$ = new Part.Index(@2.end, 0); }
| BRACKET_L expression BRACKET_R
  { $$ = new Part.Index(@3.end, $2); }
;

invokes1
: invoke
  { $$ = new ArrayList<>(List.of($1)); }
| invokes1 invoke
  { $1.add($2); $$ = $1; }
;

invoke
: DOT IDENTIFIER PAREN_L expression_invoke_arguments PAREN_R
  { $$ = new Part.Invoke(@5.end, $2, $4); }
;

expression_invoke_arguments
: %empty
  { $$ = 0; }
| expression_invoke_arguments1
  { $$ = $1.getKey(); }
;

expression_invoke_arguments1
: expression_invoke_argument
  { $$ = new SimpleEntry<>($1, $1); }
| expression_invoke_arguments1 COMMA expression_invoke_argument
  { siblingSet($1.getValue(), $3); $1.setValue($3); }
;

expression_invoke_argument
: expression
  { $$ = nodeSet(@$, new Expression.Invoke.Argument($1)); }
;

identifiers
: IDENTIFIER
  { $$ = new ArrayList<>(List.of(new Part.Access(@1.end, $1))); }
| identifiers DOT IDENTIFIER
  { $1.add(new Part.Access(@3.end, $3)); $$ = $1; }
;
