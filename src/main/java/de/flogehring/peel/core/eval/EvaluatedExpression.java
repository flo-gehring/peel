package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.TypeDescriptor;

import java.util.List;

public sealed interface EvaluatedExpression extends EvaluatedCodeElement {

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

    record FunctionCall(
            String name,
            TypeDescriptor type,
            Object value,
            List<EvaluatedExpression> arguments
    ) implements EvaluatedExpression {

    }
}
