package de.flogehring.peel.eval;

import de.flogehring.peel.lang.Expression;

public sealed interface EvaluatedExpression {

    Object value();

    TypeDescriptor type();

    record Literal(
            Object value,
            TypeDescriptor type
    ) implements EvaluatedExpression {
    }

    record BinaryOperator(
            String operator,
            TypeDescriptor type,
            Object value,
            EvaluatedExpression lhs,
            EvaluatedExpression rhs
    ) implements EvaluatedExpression {
    }

    record VariableName(
            String name,
            EvaluatedExpression backingExpression
    ) implements EvaluatedExpression {
        @Override
        public TypeDescriptor type() {
            return backingExpression.type();
        }

        @Override
        public Object value() {
            return backingExpression.value();
        }
    }
}
