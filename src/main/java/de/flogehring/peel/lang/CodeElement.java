package de.flogehring.peel.lang;

import static de.flogehring.peel.eval.TypeDescriptor.type;
import static de.flogehring.peel.util.GenericChecker.isGeneric;

public sealed interface CodeElement permits Expression, Statement {

    static Expression.BinaryOperator expr(Expression lhs, String op, Expression rhs) {
        return new Expression.BinaryOperator(op, lhs, rhs);
    }

    static Expression.Literal literal(Object literal) {
        if (isGeneric(literal)) {
            throw new RuntimeException("Can't automatically infer correct Type info of " + literal.getClass().getName());
        }
        return new Expression.Literal(type(literal.getClass()), literal);
    }

    static Expression.VariableName var(String name) {
        return new Expression.VariableName(name);
    }

    static Statement.Assignment assign(String var, Expression expr) {
        return new Statement.Assignment(var, expr);
    }
}
