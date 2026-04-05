grammar Peel;


@visitor::header {
    package de.flogehring.peel.antlr;
}


// ----------------------
// Parser Rules
// ----------------------

program
    : statement* EOF
    ;

statement
    : declaration
    | functionDeclaration
    | assignment
    | ifStatement
    | returnStatement
    | expr ';'
    | whileStatement
    | forEachStatement
    ;

declaration
 : 'var' IDENT ('=' expr)? ';'
 ;

functionDeclaration
    : 'fun' IDENT parameters block
    ;

parameters
    : '(' (IDENT (',' IDENT)*)? ')'
    ;

// TODO Clean up grammar wrinkles and inconsistencies.
// -> IfStatement vs if Expr.
// Everything is a statement
// Missing features
// Strings should be possible.
// boolean values
// map literals
// map accessors


assignment
    : IDENT '=' expr ';'
    ;

ifStatement
    : 'if' '(' expr ')' block ('else' 'if' '(' expr ')' block)* ('else' block)?
    ;

whileStatement
    : 'while' '(' expr ')' block
    ;

forEachStatement
    : 'for' '(' IDENT 'in' expr ')' block
    ;

returnStatement
    : 'return' expr? ';'
    ;

block
    : '{' statement* '}'
    | statement
    ;

// ----------------------
// Expressions
// ----------------------

// TODO Nest the Expression so they have the usual precedence rules

expr
    : 'if' '(' expr ')' block ('else' 'if' '(' expr ')' block)* ('else' block)?  # ifExpr
    | '!' expr             # notExpr
    | '[' expr ? (',' expr)* ','? ']'              # listExpr
    | '{' (expr ':' expr (',' expr ':' expr)*)? ','? '}' # mapExpr
    | 'fun' parameters block # lambdaExpr
    | '(' expr ')'         # parenExpr
    | 'True'               # trueExpr
    | 'False'              # falseExpr
    | STRING               # stringExpr
    | DECIMAL              # decimalExpr
    | INTEGER              # numberExpr
    | IDENT                # varExpr
    | expr arguments       # functionCallExpr
    | expr '[' expr ']'    # selectorExpr
    | <assoc=right> expr '**' expr # powExpr
    | '-' expr             # negateExpr
    | expr ('*' | '/' | '%') expr # mulDivExpr
    | expr ('+' | '-') expr # addSubExpr
    | expr ('==' | '!=') expr       # eqExpr
    | expr '&&' expr       # logicalAndExpr
    | expr '^' expr        # xorExpr
    | expr '||' expr       # logicalOrExpr
    | <assoc=right> expr '?' expr ':' expr # ternaryExpr
    ;

arguments:
    '(' (expr (',' expr)*)?')'
    ;

// ----------------------
// Lexer Rules
// ----------------------

IDENT
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

DECIMAL
    : [0-9]+ '.' [0-9]+
    ;

INTEGER
    : [0-9]+
    ;

STRING
    : '"' ( '\\' ["\\nrt] | ~["\\\r\n] )* '"'
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

COMMENT
    : '//' ~[\r\n]* -> skip
    ;
