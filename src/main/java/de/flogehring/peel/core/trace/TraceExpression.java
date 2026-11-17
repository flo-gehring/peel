package de.flogehring.peel.core.trace;

import de.flogehring.peel.core.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        TraceExpression.Block {

    TraceValue value();
    TraceExpressionKind kind();

    TraceExpression.Block EMPTY_BLOCK = new TraceExpression.Block(List.of());

    record Literal(TraceValue value) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.LITERAL;
        }
    }

    record BinaryOperator(
            String operator,
            TraceValue value,
            TraceExpression lhs,
            @Nullable TraceExpression rhs,
            boolean didShortCircuit
    ) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.BINARY_OPERATOR;
        }
    }

    record UnaryPrefixOperator(
            String operator,
            TraceValue value,
            TraceExpression argument
    ) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.UNARY_PREFIX_OPERATOR;
        }
    }

    record VariableName(
            String name,
            TraceValue value
    ) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.VARIABLE_NAME;
        }
    }

    record FunctionCall(
            String name,
            TraceValue value,
            List<TraceExpression> arguments,
            Optional<FunctionExecutionTrace> subEvaluation,
            CalleeSource calleeSource,
            ResolvedCallable resolvedCallable
    ) implements TraceExpression {
        public FunctionCall {
            Objects.requireNonNull(calleeSource, "calleeSource");
            Objects.requireNonNull(resolvedCallable, "resolvedCallable");
        }

        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.FUNCTION_CALL;
        }
    }

    sealed interface CalleeSource permits CalleeSource.VariableName, CalleeSource.Expression {
        record VariableName(String varName) implements CalleeSource {
        }

        record Expression(TraceExpression expression) implements CalleeSource {
        }

        static CalleeSource variable(String variableName) {
            return new VariableName(variableName);
        }

        static CalleeSource expression(TraceExpression expression) {
            return new Expression(expression);
        }
    }

    record ResolvedCallable(
            CallableKind kind,
            String name,
            int arity
    ) {
    }

    record FunctionExecutionTrace(
            List<ParameterBinding> parameterBindings,
            Block bodyEvaluation
    ) {
    }

    record ParameterBinding(String name, TraceExpression argument) {
    }

    record ReturnExpr(TraceValue value, TraceExpression expression) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.RETURN_EXPR;
        }
    }

    record Assignment(String variableName, TraceExpression expression, TraceValue value) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.ASSIGNMENT;
        }
    }

    record IfStatement(
            List<TraceExpression> conditions,
            TraceExpression executedBlock
    ) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.IF_STATEMENT;
        }

        @Override
        public TraceValue value() {
            return executedBlock != null ? executedBlock.value() : TraceValue.NONE_VALUE;
        }
    }

    record WhileLoop(List<Iteration> iterations, TraceValue value) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.WHILE_LOOP;
        }

        public record Iteration(
                TraceExpression condition,
                Block body
        ) {
        }
    }

    record ForEachLoop(
            String variableName,
            TraceExpression iterableExpression,
            List<Iteration> iterations
    ) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.FOR_EACH_LOOP;
        }

        public record Iteration(
                VariableBinding binding,
                Block body
        ) {
        }

        public record VariableBinding(String name, TraceValue value) {
        }

        @Override
        public TraceValue value() {
            return iterations.isEmpty() ? TraceValue.NONE_VALUE : iterations.getLast().body().value();
        }
    }

    record ListLiteral(
            List<TraceExpression> elements,
            TraceValue value
    ) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.LIST_LITERAL;
        }
    }

    record MapLiteral(
            List<MapEntry> entries
    ) implements TraceExpression {
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.MAP_LITERAL;
        }

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
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.SELECTOR;
        }
    }

    record Block(List<TraceExpression> content, String name, Map<String, TraceValue> variablesAtExit) implements TraceExpression {
        public Block {
            variablesAtExit = Map.copyOf(variablesAtExit);
        }

        public Block(List<TraceExpression> content, String name) {
            this(content, name, Map.of());
        }

        public Block(List<TraceExpression> content) {
            this(content, null, Map.of());
        }
        @Override
        public TraceExpressionKind kind() {
            return TraceExpressionKind.BLOCK;
        }

        @Override
        public TraceValue value() {
            return content.isEmpty() ? TraceValue.none() : content.getLast().value();
        }
    }
}
