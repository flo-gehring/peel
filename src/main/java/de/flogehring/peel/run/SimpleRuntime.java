package de.flogehring.peel.run;

import de.flogehring.peel.eval.Runtime;
import de.flogehring.peel.eval.*;
import de.flogehring.peel.lang.CodeElement;
import de.flogehring.peel.lang.Expression;
import de.flogehring.peel.lang.Program;
import de.flogehring.peel.lang.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static de.flogehring.peel.eval.TypeDescriptor.type;

public class SimpleRuntime implements Runtime {

    private final HashMap<String, EvaluatedExpression> variables;
    private final HashMap<String, List<Function>> functions;

    public static SimpleRuntime simpleLang() {
        SimpleRuntime runtime = new SimpleRuntime();
        runtime.register(addNumbers());
        runtime.register(addStrings());
        runtime.register(countSubstring());
        return runtime;
    }

    private SimpleRuntime() {
        functions = new HashMap<>();
        variables = new HashMap<>();
    }

    @Override
    public void register(Variable v) {
        variables.put(v.name(), new EvaluatedExpression.VariableName(
                v.name(),
                v.value()
        ));
    }

    @Override
    public void register(Function f) {
        functions.merge(f.name(), new ArrayList<>(List.of(f)), (lhs, rhs) -> Stream.concat(
                lhs.stream(),
                rhs.stream()
        ).toList());
    }

    private static Function addStrings() {
        return new Function() {
            @Override
            public String name() {
                return "+";
            }

            @Override
            public List<TypeDescriptor> arguments() {
                return List.of(type(String.class), type(String.class));
            }

            @Override
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                String lhs = (String) arguments[0].value();
                String rhs = (String) arguments[1].value();
                return new EvaluatedExpression.BinaryOperator(
                        "+",
                        type(String.class),
                        lhs + rhs,
                        arguments[0],
                        arguments[1]
                );
            }
        };
    }

    private static Function countSubstring() {
        return new Function() {
            @Override
            public String name() {
                return "count";
            }

            @Override
            public List<TypeDescriptor> arguments() {
                return List.of(
                        type(String.class),
                        type(String.class)
                );
            }

            @Override
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                EvaluatedExpression lhsExpression = arguments[0];
                EvaluatedExpression rhsExpression = arguments[1];
                String lhs = (String) lhsExpression.value();
                String rhs = (String) rhsExpression.value();
                int occurences = 0;
                while (lhs.contains(rhs)) {
                    occurences++;
                    lhs = lhs.replaceFirst(rhs, "");
                }
                return new EvaluatedExpression.FunctionCall(
                        "count",
                        type(Number.class),
                        occurences,
                        Arrays.stream(arguments).toList()
                );
            }
        };
    }

    private static Function addNumbers() {
        return new Function() {
            @Override
            public String name() {
                return "+";
            }

            @Override
            public List<TypeDescriptor> arguments() {
                return List.of(type(Number.class), type(Number.class));
            }

            @Override
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                EvaluatedExpression lhsExpression = arguments[0];
                EvaluatedExpression rhsExpression = arguments[1];
                Number lhs = (Number) lhsExpression.value();
                Number rhs = (Number) rhsExpression.value();
                return new EvaluatedExpression.BinaryOperator(
                        "+",
                        type(Number.class),
                        lhs.doubleValue() + rhs.doubleValue(),
                        lhsExpression, rhsExpression
                );
            }
        };
    }

    @Override
    public EvaluatedProgram run(Program program) {
        List<EvaluatedCodeElement> evaluatedCodeElements = new ArrayList<>(program.codeElement().size());
        for (int i = 0; i < program.codeElement().size(); ++i) {
            CodeElement codeElement = program.codeElement().get(i);
            EvaluatedCodeElement evaluatedCodeElement = switch (codeElement) {
                case Expression expression -> evaluateExpr(expression);
                case Statement statement -> runStatement(statement);
            };
            evaluatedCodeElements.add(evaluatedCodeElement);
        }
        return new EvaluatedProgram(evaluatedCodeElements);
    }

    private EvaluatedStatement runStatement(Statement statement) {
        return switch (statement) {
            case Statement.Assignment(var name, var expression) -> {
                EvaluatedExpression value = evaluateExpr(expression);
                variables.put(name, value);
                yield new EvaluatedStatement.Assignment(
                        name, value
                );
            }
        };
    }

    private EvaluatedExpression evaluateExpr(Expression expression) {
        return switch (expression) {
            case Expression.BinaryOperator operator -> evaluateOperator(
                    operator
            );
            case Expression.Literal(var typeDescriptor, var literal) ->
                    new EvaluatedExpression.Literal(literal, typeDescriptor);
            case Expression.VariableName(var name) -> variables.get(name);
            case Expression.FunctionCall functionCall -> evaluateFunction(functionCall);
        };
    }

    private EvaluatedExpression evaluateFunction(Expression.FunctionCall functionCall) {
        List<EvaluatedExpression> arguments = functionCall.arguments().stream()
                .map(this::evaluateExpr)
                .toList();
        List<Function> matchingName = functions.get(functionCall.functionName());
        List<Function> list = matchingName.stream()
                .filter(f -> argumentsFit(f.arguments(), arguments))
                .toList();
        Function f = requireOneFunction(
                list,
                getNoFunctionFoundException(functionCall.functionName(), arguments.toArray()),
                getMultipleFunctionsFoundException(functionCall.functionName(), list)
        );
        return f.run(arguments.toArray(new EvaluatedExpression[0]));
    }

    private EvaluatedExpression evaluateOperator(Expression.BinaryOperator operator) {
        List<Function> matchingName = functions.get(operator.operator());
        EvaluatedExpression lhs = evaluateExpr(operator.lhs());
        EvaluatedExpression rhs = evaluateExpr(operator.rhs());
        List<Function> list = matchingName.stream()
                .filter(f -> argumentsFit(f.arguments(), List.of(lhs, rhs)))
                .toList();
        Function f = requireOneFunction(
                list,
                getNoFunctionFoundException(operator.operator(), lhs, rhs),
                getMultipleFunctionsFoundException(operator.operator(), list)
        );
        return f.run(lhs, rhs);
    }

    private static NoFunctionFoundException getNoFunctionFoundException(String operator, Object... arguments) {
        return new NoFunctionFoundException(operator, arguments);
    }

    private MultipleFunctionsFoundException getMultipleFunctionsFoundException(String operator, List<Function> list) {
        return new MultipleFunctionsFoundException(operator, list.size());
    }

    private Function requireOneFunction(List<Function> list, NoFunctionFoundException e, MultipleFunctionsFoundException multipleFunctionsFoundException) {
        if (list.isEmpty()) {
            throw e;
        } else if (list.size() > 1) {
            throw multipleFunctionsFoundException;
        } else {
            return list.getFirst();
        }
    }

    private boolean argumentsFit(List<TypeDescriptor> arguments, List<EvaluatedExpression> lhs) {
        boolean result = arguments.size() == lhs.size();
        if (result) {
            for (int i = 0; i < arguments.size() && result; ++i) {
                TypeDescriptor typeDescriptor = arguments.get(i);
                TypeDescriptor arg = lhs.get(i).type();
                result = matches(typeDescriptor, arg);
            }
        }
        return result;
    }

    private boolean matches(TypeDescriptor typeDescriptor, TypeDescriptor arg) {
        return switch (typeDescriptor) {
            case TypeDescriptor.Type(var t) -> switch (arg) {
                case TypeDescriptor.ListOf<?> _ -> false;
                case TypeDescriptor.Type(var t2) -> t.isAssignableFrom(t2);
            };
            case TypeDescriptor.ListOf(var t1) -> switch (arg) {
                case TypeDescriptor.ListOf(var t2) -> t1.isAssignableFrom(t2);
                case TypeDescriptor.Type(var _) -> false;
            };
        };
    }
}