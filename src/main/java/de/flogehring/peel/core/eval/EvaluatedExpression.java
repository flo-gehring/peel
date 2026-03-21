package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.PeelCallable;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.EvaluationEnvironment;
import de.flogehring.peel.run.exceptions.PeelException;

import java.util.List;
import java.util.Optional;

public sealed interface EvaluatedExpression {

    PeelValue value();

    static Literal peelLiteral(PeelValue value) {
        return new Literal(value);
    }

    record Literal(
            PeelValue peelValue
    ) implements EvaluatedExpression {

        @Override
        public String toString() {
            return peelValue.toString();
        }

        @Override
        public PeelValue value() {
            return peelValue;
        }
    }

    record Closure( // TODO Closure can be removed because i introduced the closure value
                    PeelCallable callable,
                    EvaluationEnvironment environment
    ) implements EvaluatedExpression {
        @Override
        public PeelValue value() {
            return callable;
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
            PeelValue value
    ) implements EvaluatedExpression {

        @Override
        public String toString() {
            return name + " <-> " + value.toString();
        }
    }

    record FunctionCall(
            String name,
            PeelValue value,
            List<EvaluatedExpression> arguments
    ) implements EvaluatedExpression {

        @Override
        public String toString() {
            return value.toString() + " <-> `" + name + "`(" + String.join(", ", arguments.stream().map(EvaluatedExpression::toString).toList()) + ")";
        }

    }

    record Return(EvaluatedExpression toReturn) implements EvaluatedExpression {
        @Override
        public PeelValue value() {
            return toReturn.value();
        }
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

        public record Iteration(
                EvaluatedExpression condition,
                Optional<EvaluatedBlock> evaluatedBlock
        ) {
        }
    }

    record ForEachLoop(List<ForEachLoop.Iteration> iterations) implements EvaluatedExpression {

        @Override
        public PeelValue value() {
            return iterations.isEmpty() ? EvaluatedBlock.empty().value() : iterations.getLast().body.value();
        }

        public record Iteration(PeelValue val, EvaluatedBlock body) {
        }
    }

    record EvaluatedListLiteral(
            List<EvaluatedExpression> elements,
            PeelValue.Collection.List value
    ) implements EvaluatedExpression {
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