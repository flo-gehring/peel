package de.flogehring.peel.lang;

public sealed interface CodeElement permits Expression, Statement {

    static Expression.BinaryOperator expr(Expression lhs, String op, Expression rhs) {
        return new Expression.BinaryOperator(op, lhs, rhs);
    }

    static Expression.Literal literal(Object literal) {
        return new Expression.Literal(literal);
    }

    static Expression.VariableName var(String name) {
        return new Expression.VariableName(name);
    }

    static Statement.Assignment assign(String var, Expression expr) {
        return new Statement.Assignment(var, expr);
    }
}
