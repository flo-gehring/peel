grammar Peel;


@header {
    package de.flogehring.peel.antlr;
}


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
    : assignment
    | ifStatement
    | expr ';'
    ;

assignment
    : IDENT '=' expr ';'
    ;

ifStatement
    : 'if' '(' expr ')' block ('else' 'if' '(' expr ')' block)* ('else' block)?
    ;

block
    : '{' statement* '}'
    | statement
    ;

// ----------------------
// Expressions
// ----------------------

expr
    : expr '||' expr       # logicalOrExpr
    | expr '&&' expr       # logicalAndExpr
    | expr '==' expr       # eqExpr
    | expr '^' expr        # xorExpr
    | '!' expr             # notExpr
    | expr ('*' | '/') expr # mulDivExpr
    | expr ('+' | '-') expr # addSubExpr
    | '(' expr ')'         # parenExpr
    | IDENT                # varExpr
    | NUMBER               # numberExpr
    ;

// ----------------------
// Lexer Rules
// ----------------------

IDENT
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

NUMBER
    : [0-9]+ ('.' [0-9]+)?
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

COMMENT
    : '//' ~[\r\n]* -> skip
    ;
