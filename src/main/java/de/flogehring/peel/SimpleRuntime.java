package de.flogehring.peel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

public class SimpleRuntime implements Runtime {

    Map<String, BiFunction<Object, Object, Object>> operators;
    Map<String, Object> variables;


    private SimpleRuntime() {
        operators = new HashMap<>();
        variables = new HashMap<>();
    }

    public static SimpleRuntime simpleLang() {
        SimpleRuntime runtime = new SimpleRuntime();
        runtime.operators.put("+", (x, y) -> ((Number) x).doubleValue() + ((Number) y).doubleValue());
        return runtime;
    }

    @Override
    public Object run(Programm programm) {
        Optional<Object> result = Optional.empty();
        for (int i = 0; i < programm.codeElement().size(); ++i) {
            CodeElement codeElement = programm.codeElement().get(i);
            result = switch (codeElement) {
                case Expression expression -> Optional.of(evaluateExpr(expression));
                case Statement statement -> {
                    runStatement(statement);
                    yield Optional.empty();
                }
            };
        }
        return result.orElseThrow(() -> new RuntimeException("Programm did not contain any top level expression to return"));
    }

    private void runStatement(Statement statement) {
         switch (statement) {
            case Statement.Assignment(var name, var expression) -> variables.put(name, evaluateExpr(expression));
        }
    }

    private Object evaluateExpr(Expression expression) {
        return switch (expression){
            case Expression.BinaryOperator operator -> evaluateOperator(
                    operator
            );
            case Expression.Literal(var literal) -> literal ;
            case Expression.VariableName(var name) -> variables.get(name);
        };
    }

    private Object evaluateOperator(Expression.BinaryOperator operator) {
        BiFunction<Object, Object, Object> biFunction = Objects.requireNonNull(operators.get(operator.operator()), "Could not find Operator " + operator.operator());
        return biFunction.apply(evaluateExpr(operator.lhs()), evaluateExpr(operator.rhs()));
    }
}
