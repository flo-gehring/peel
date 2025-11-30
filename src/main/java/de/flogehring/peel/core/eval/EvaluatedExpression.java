package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.PeelValue;

import java.util.List;

public sealed interface EvaluatedExpression {

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

    record Assignment(String variableName, EvaluatedExpression expression) implements EvaluatedExpression {
        @Override
        public PeelValue value() {
            return expression.value();
        }
    }

    record IfStatement(
            EvaluatedExpression condition,
            EvaluatedBlock executedBlock
    ) implements EvaluatedExpression {
        @Override
        public PeelValue value() {
            return executedBlock.value();
        }
    }

    record EvaluatedBlock(List<EvaluatedExpression> content) implements EvaluatedExpression {

        private static final EvaluatedBlock EVALUATED_BLOCK = new EvaluatedBlock(List.of());

        public static EvaluatedBlock empty() {
            return EVALUATED_BLOCK;
        }

        @Override
        public PeelValue value() {
            if (content.isEmpty()) {
                return None.NONE;
            }
            return content.getLast().value();
        }
    }

}
