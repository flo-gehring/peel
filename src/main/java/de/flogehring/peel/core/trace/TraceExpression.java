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
            TraceExpression executedBlock,
            TraceValue value
    ) implements TraceExpression {
    }

    record WhileLoop(List<Iteration> iterations, TraceValue value) implements TraceExpression {
        public record Iteration(
                TraceExpression condition,
                Block body
        ) {
        }
    }

    record ForEachLoop(List<Iteration> iterations, TraceValue value) implements TraceExpression {
        public record Iteration(TraceValue itemValue, Block body) {
        }
    }

    record ListLiteral(
            List<TraceExpression> elements,
            TraceValue value
    ) implements TraceExpression {
    }

    record MapLiteral(
            List<MapEntry> entries,
            TraceValue value
    ) implements TraceExpression {
        public record MapEntry(TraceExpression key, TraceExpression value) {
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
