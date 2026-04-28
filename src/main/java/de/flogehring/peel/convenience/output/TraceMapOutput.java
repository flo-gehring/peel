package de.flogehring.peel.convenience.output;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TraceMapOutput {

    private TraceMapOutput() {
    }

    public static Map<String, Object> fromProgram(TraceProgram traceProgram) {
        Objects.requireNonNull(traceProgram, "traceProgram");
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("type", "program");
        node.put("expressions", traceProgram.expressions().stream().map(TraceMapOutput::mapExpression).toList());
        node.put("result", mapValue(traceProgram.result()));
        return node;
    }

    public static Map<String, Object> fromExpression(TraceExpression traceExpression) {
        Objects.requireNonNull(traceExpression, "traceExpression");
        return mapExpression(traceExpression);
    }

    public static Map<String, Object> fromValue(TraceValue traceValue) {
        Objects.requireNonNull(traceValue, "traceValue");
        return mapValue(traceValue);
    }

    private static Map<String, Object> mapExpression(TraceExpression traceExpression) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("value", mapValue(traceExpression.value()));

        switch (traceExpression) {
            case TraceExpression.Literal _ -> node.put("type", "literal");
            case TraceExpression.BinaryOperator(
                    String operator,
                    _,
                    TraceExpression lhs,
                    TraceExpression rhs,
                    boolean didShortCircuit
            ) -> {
                node.put("type", "binary_operator");
                node.put("operator", operator);
                node.put("lhs", mapExpression(lhs));
                node.put("rhs", rhs == null ? null : mapExpression(rhs));
                node.put("didShortCircuit", didShortCircuit);
            }
            case TraceExpression.UnaryPrefixOperator(
                    String operator,
                    _,
                    TraceExpression argument
            ) -> {
                node.put("type", "unary_prefix_operator");
                node.put("operator", operator);
                node.put("argument", mapExpression(argument));
            }
            case TraceExpression.VariableName(String name, _) -> {
                node.put("type", "variable_name");
                node.put("name", name);
            }
            case TraceExpression.FunctionCall(
                    String name,
                    _,
                    List<TraceExpression> arguments,
                    java.util.Optional<TraceExpression.FunctionExecutionTrace> subEvaluation,
                    TraceExpression.CalleeSource calleeSource,
                    TraceExpression.ResolvedCallable resolvedCallable
            ) -> {
                node.put("type", "function_call");
                node.put("name", name);
                node.put("arguments", arguments.stream().map(TraceMapOutput::mapExpression).toList());
                node.put("subEvaluation", subEvaluation.map(TraceMapOutput::mapFunctionExecutionTrace).orElse(null));
                node.put("calleeSource", mapCalleeSource(calleeSource));
                node.put("resolvedCallable", mapResolvedCallable(resolvedCallable));
            }
            case TraceExpression.ReturnExpr(_, TraceExpression expression) -> {
                node.put("type", "return_expression");
                node.put("expression", mapExpression(expression));
            }
            case TraceExpression.Assignment(String variableName, TraceExpression expression, _) -> {
                node.put("type", "assignment");
                node.put("variableName", variableName);
                node.put("expression", mapExpression(expression));
            }
            case TraceExpression.IfStatement(List<TraceExpression> conditions, TraceExpression executedBlock) -> {
                node.put("type", "if_statement");
                node.put("conditions", conditions.stream().map(TraceMapOutput::mapExpression).toList());
                node.put("executedBlock", executedBlock == null ? null : mapExpression(executedBlock));
            }
            case TraceExpression.WhileLoop(List<TraceExpression.WhileLoop.Iteration> iterations, _) -> {
                node.put("type", "while_loop");
                node.put("iterations", iterations.stream().map(TraceMapOutput::mapWhileIteration).toList());
            }
            case TraceExpression.ForEachLoop(List<TraceExpression.ForEachLoop.Iteration> iterations) -> {
                node.put("type", "for_each_loop");
                node.put("iterations", iterations.stream().map(TraceMapOutput::mapForEachIteration).toList());
            }
            case TraceExpression.ListLiteral(List<TraceExpression> elements, _) -> {
                node.put("type", "list_literal");
                node.put("elements", elements.stream().map(TraceMapOutput::mapExpression).toList());
            }
            case TraceExpression.MapLiteral(List<TraceExpression.MapLiteral.MapEntry> entries) -> {
                node.put("type", "map_literal");
                node.put("entries", entries.stream().map(TraceMapOutput::mapMapLiteralEntry).toList());
            }
            case TraceExpression.Selector(_, TraceExpression target, TraceExpression selector) -> {
                node.put("type", "selector");
                node.put("target", mapExpression(target));
                node.put("selector", mapExpression(selector));
            }
            case TraceExpression.Block(List<TraceExpression> content) -> {
                node.put("type", "block");
                node.put("content", content.stream().map(TraceMapOutput::mapExpression).toList());
            }
        }
        return node;
    }

    private static Map<String, Object> mapFunctionExecutionTrace(TraceExpression.FunctionExecutionTrace functionExecutionTrace) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put(
                "parameterBindings",
                functionExecutionTrace.parameterBindings().stream().map(TraceMapOutput::mapParameterBinding).toList()
        );
        node.put("bodyEvaluation", mapExpression(functionExecutionTrace.bodyEvaluation()));
        return node;
    }

    private static Map<String, Object> mapCalleeSource(TraceExpression.CalleeSource calleeSource) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        switch (calleeSource) {
            case TraceExpression.CalleeSource.VariableName(String varName) -> {
                node.put("kind", "variable");
                node.put("variableName", varName);
            }
            case TraceExpression.CalleeSource.Expression(TraceExpression expression) -> {
                node.put("kind", "expression");
                node.put("expression", mapExpression(expression));
            }
        }
        return node;
    }

    private static Map<String, Object> mapResolvedCallable(TraceExpression.ResolvedCallable resolvedCallable) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("kind", resolvedCallable.kind().wireValue());
        node.put("name", resolvedCallable.name());
        node.put("arity", resolvedCallable.arity());
        return node;
    }

    private static Map<String, Object> mapParameterBinding(TraceExpression.ParameterBinding parameterBinding) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("name", parameterBinding.name());
        node.put("argument", mapExpression(parameterBinding.argument()));
        return node;
    }

    private static Map<String, Object> mapWhileIteration(TraceExpression.WhileLoop.Iteration iteration) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("condition", mapExpression(iteration.condition()));
        node.put("body", mapExpression(iteration.body()));
        return node;
    }

    private static Map<String, Object> mapForEachIteration(TraceExpression.ForEachLoop.Iteration iteration) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("body", mapExpression(iteration.body()));
        return node;
    }

    private static Map<String, Object> mapMapLiteralEntry(TraceExpression.MapLiteral.MapEntry mapEntry) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("key", mapExpression(mapEntry.key()));
        node.put("value", mapExpression(mapEntry.value()));
        return node;
    }

    private static Map<String, Object> mapValue(TraceValue traceValue) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        switch (traceValue) {
            case TraceValue.IntegerValue(int value) -> {
                node.put("type", "integer");
                node.put("value", value);
            }
            case TraceValue.FloatValue(float value) -> {
                node.put("type", "float");
                node.put("value", value);
            }
            case TraceValue.DecimalValue(String value) -> {
                node.put("type", "decimal");
                node.put("value", value);
            }
            case TraceValue.TextValue(String value) -> {
                node.put("type", "text");
                node.put("value", value);
            }
            case TraceValue.BoolValue(boolean value) -> {
                node.put("type", "bool");
                node.put("value", value);
            }
            case TraceValue.NoneValue _ -> {
                node.put("type", "none");
                node.put("value", null);
            }
            case TraceValue.ListValue(List<TraceValue> items) -> {
                node.put("type", "list");
                node.put("items", items.stream().map(TraceMapOutput::mapValue).toList());
            }
            case TraceValue.MapValue(List<TraceValue.MapValue.MapEntry> entries) -> {
                node.put("type", "map");
                node.put("entries", entries.stream().map(TraceMapOutput::mapMapValueEntry).toList());
            }
            case TraceValue.CallableRef(
                    de.flogehring.peel.core.trace.CallableKind callableKind, String name, List<String> arities
            ) -> {
                node.put("type", "callable_ref");
                node.put("callableKind", callableKind.wireValue());
                node.put("name", name);
                node.put("arities", arities);
            }
        }
        return node;
    }

    private static Map<String, Object> mapMapValueEntry(TraceValue.MapValue.MapEntry mapEntry) {
        LinkedHashMap<String, Object> node = new LinkedHashMap<>();
        node.put("key", mapValue(mapEntry.key()));
        node.put("value", mapValue(mapEntry.value()));
        return node;
    }
}
