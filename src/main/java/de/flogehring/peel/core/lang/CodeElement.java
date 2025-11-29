package de.flogehring.peel.core.lang;

import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.Text;

public sealed interface CodeElement permits Expression {

    static Expression.BinaryOperator expr(Expression lhs, String op, Expression rhs) {
        return new Expression.BinaryOperator(op, lhs, rhs);
    }

    static Expression.Literal integer(Integer literal) {

        return new Expression.Literal(new Number.Integer(literal));
    }

    static Expression.Literal string(String s) {
        return new Expression.Literal(new Text(s));
    }

    static Expression.VariableName var(String name) {
        return new Expression.VariableName(name);
    }

    static Expression.Assignment assign(String var, Expression expr) {
        return new Expression.Assignment(var, expr);
    }

    static Expression ifExpression(
            Expression condition,
            Expression.Block thenBlock,
            Expression.Block elseBlock
    ) {
        return new Expression.IfElseStatement(
                condition,
                thenBlock,
                elseBlock
        );
    }

    static Expression ifExpression(
            Expression condition,
            Expression.Block thenBlock
    ) {
        return new Expression.IfStatement(
                condition,
                thenBlock
        );
    }
}
