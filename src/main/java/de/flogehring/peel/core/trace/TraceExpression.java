package de.flogehring.peel.core.trace;

import java.util.List;
import java.util.Optional;

public sealed interface TraceExpression permits
        TraceExpression.Literal,
        TraceExpression.BinaryOperator,
        TraceExpression.UnaryPrefixOperator,
        TraceExpression.VariableName,
        TraceExpression.FunctionCall,
        TraceExpression.ReturnExpr,
        TraceExpression.Assignment,
        TraceExpression.IfStatement,
        TraceExpression.WhileLoop,
        TraceExpression.ForEachLoop,
        TraceExpression.ListLiteral,
        TraceExpression.MapLiteral,
        TraceExpression.Selector,
        TraceExpression.LogicalBinaryOperator,
        TraceExpression.Block {

    TraceValue value();

    record Literal(TraceValue value) implements TraceExpression {
    }

    record BinaryOperator(
            String operator,
            TraceValue value,
            TraceExpression lhs,
            TraceExpression rhs
    ) implements TraceExpression {
    }

    record UnaryPrefixOperator(
            String operator,
            TraceValue value,
            TraceExpression argument
    ) implements TraceExpression {
    }

    record VariableName(
            String name,
            TraceValue value
    ) implements TraceExpression {
    }

    record FunctionCall(
            String name,
            TraceValue value,
            List<TraceExpression> arguments,
            Optional<FunctionExecutionTrace> subEvaluation
    ) implements TraceExpression {
    }

    record FunctionExecutionTrace(
            String calleeKind,
            List<ParameterBinding> parameterBindings,
            Block bodyEvaluation
    ) {
    }

    record ParameterBinding(String name, TraceExpression argument) {
    }

    record ReturnExpr(TraceValue value, TraceExpression expression) implements TraceExpression {
    }

    record Assignment(String variableName, TraceExpression expression, TraceValue value) implements TraceExpression {
    }

    record IfStatement(
            List<TraceExpression> conditions,
            TraceExpression executedBlock
    ) implements TraceExpression {
        @Override
        public TraceValue value() {
            return executedBlock != null ? executedBlock.value() : new TraceValue.NoneValue();
        }
    }

    record WhileLoop(List<Iteration> iterations, TraceValue value) implements TraceExpression {
        public record Iteration(
                TraceExpression condition,
                Block body
        ) {
        }
    }

    record ForEachLoop(List<Iteration> iterations) implements TraceExpression {
        public record Iteration(Block body) {
        }

        @Override
        public TraceValue value() {
            return iterations.isEmpty() ? new TraceValue.NoneValue() : iterations.getLast().body().value();
        }
    }

    record ListLiteral(
            List<TraceExpression> elements,
            TraceValue value
    ) implements TraceExpression {
    }

    record MapLiteral(
            List<MapEntry> entries
    ) implements TraceExpression {
        public record MapEntry(TraceExpression key, TraceExpression value) {
        }

        @Override
        public TraceValue value() {
            return new TraceValue.MapValue(
                    entries.stream().map(
                            entry -> new TraceValue.MapValue.MapEntry(
                                    entry.key.value(),
                                    entry.value.value()
                            )
                    ).toList()
            );
        }
    }

    record Selector(
            TraceValue value,
            TraceExpression target,
            TraceExpression selector
    ) implements TraceExpression {
    }

    record LogicalBinaryOperator(
            String operator,
            TraceValue value,
            TraceExpression lhs,
            Optional<TraceExpression> rhs,
            boolean shortCircuited
    ) implements TraceExpression {
    }

    record Block(List<TraceExpression> content) implements TraceExpression {
        @Override
        public TraceValue value() {
            return content.isEmpty() ? TraceValue.none() : content.getLast().value();
        }
    }
}
