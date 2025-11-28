package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.PeelValue;

import java.util.List;

public sealed interface EvaluatedExpression extends EvaluatedCodeElement {

    PeelValue value();

    record Literal(
            PeelValue peelValue
    ) implements EvaluatedExpression {
        @Override
        public PeelValue value() {
            return peelValue;
        }
    }

    record BinaryOperator(
            String operator,
            PeelValue value,
            EvaluatedExpression lhs,
            EvaluatedExpression rhs
    ) implements EvaluatedExpression {
    }

    record VariableName(
            String name,
            EvaluatedExpression backingExpression
    ) implements EvaluatedExpression {

        @Override
        public PeelValue value() {
            return backingExpression.value();
        }
    }

    record FunctionCall(
            String name,
            PeelValue value,
            List<EvaluatedExpression> arguments
    ) implements EvaluatedExpression {

    }
}
