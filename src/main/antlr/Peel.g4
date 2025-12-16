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
    | whileStatement
    ;

assignment
    : IDENT '=' expr ';'
    ;

ifStatement
    : 'if' '(' expr ')' block ('else' 'if' '(' expr ')' block)* ('else' block)?
    ;

whileStatement
    : 'while' '(' expr ')' block
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
    | expr '||' expr       # logicalOrExpr
    | expr '&&' expr       # logicalAndExpr
    | expr '==' expr       # eqExpr
    | expr '^' expr        # xorExpr
    | '!' expr             # notExpr
    | expr ('*' | '/') expr # mulDivExpr
    | expr ('+' | '-') expr # addSubExpr
    | nonTernaryExpr '?' nonTernaryExpr ':' nonTernaryExpr # ternaryExpr
    | '(' expr ')'         # parenExpr
    | IDENT                # varExpr
    | NUMBER               # numberExpr
    ;

nonTernaryExpr
    : 'if' '(' nonTernaryExpr ')' block ('else' 'if' '(' nonTernaryExpr ')' block)* ('else' block)?  # nonTernaryIfExpr
    | nonTernaryExpr '||' nonTernaryExpr       # nonTernaryLogicalOrExpr
    | nonTernaryExpr '&&' nonTernaryExpr       # nonTernaryLogicalAndExpr
    | nonTernaryExpr '==' nonTernaryExpr       # nonTernaryEqExpr
    | nonTernaryExpr '^' nonTernaryExpr        # nonTernaryXorExpr
    | '!' nonTernaryExpr             # nonTernaryNotExpr
    | nonTernaryExpr ('*' | '/') nonTernaryExpr # nonTernaryMulDivExpr
    | nonTernaryExpr ('+' | '-') nonTernaryExpr # nonTernaryAddSubExpr
    | '(' nonTernaryExpr ')'         # nonTernaryParenExpr
    | IDENT                # nonTernaryVarExpr
    | NUMBER               # nonTernaryNumberExpr
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
