package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.PeelException;

import java.util.List;
import java.util.Optional;

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

    record UnaryPrefixOperator(
            String operator,
            PeelValue value,
            EvaluatedExpression argument
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
            EvaluatedExpression executedBlock
    ) implements EvaluatedExpression {
        @Override
        public PeelValue value() {
            return executedBlock.value();
        }
    }

    record WhileLoop(List<Iteration> iterations) implements EvaluatedExpression {

        @Override
        public PeelValue value() {
            return iterations.size() == 1
                    ? EvaluatedBlock.empty().value()
                    : iterations.get(iterations.size() - 2)
                    .evaluatedBlock.orElseThrow(
                            () -> new PeelException("Second to last iteration of a while loop is expected to have an evaluated body.")
                    ).value();
        }

        public record Iteration(EvaluatedExpression condition, Optional<EvaluatedBlock> evaluatedBlock) {

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
