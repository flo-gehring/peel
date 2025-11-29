package de.flogehring.peel.core.lang;

import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.Text;

public class ExpressionFactoryMethods {

    private ExpressionFactoryMethods() {

    }

    public static Expression.BinaryOperator expr(Expression lhs, String op, Expression rhs) {
        return new Expression.BinaryOperator(op, lhs, rhs);
    }

    public static Expression.Literal integer(Integer literal) {

        return new Expression.Literal(new Number.Integer(literal));
    }

    public static Expression.Literal string(String s) {
        return new Expression.Literal(new Text(s));
    }

    public static Expression.VariableName var(String name) {
        return new Expression.VariableName(name);
    }

    public static Expression.Assignment assign(String var, Expression expr) {
        return new Expression.Assignment(var, expr);
    }

    public static Expression ifExpression(
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

    public static Expression ifExpression(
            Expression condition,
            Expression.Block thenBlock
    ) {
        return new Expression.IfStatement(
                condition,
                thenBlock
        );
    }
}
