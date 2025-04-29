package de.flogehring.peel.run;

import de.flogehring.peel.eval.Function;
import de.flogehring.peel.eval.Runtime;
import de.flogehring.peel.eval.TypeDescriptor;
import de.flogehring.peel.eval.Variable;
import de.flogehring.peel.lang.CodeElement;
import de.flogehring.peel.lang.Expression;
import de.flogehring.peel.lang.Program;
import de.flogehring.peel.lang.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static de.flogehring.peel.eval.TypeDescriptor.type;

public class SimpleRuntime implements Runtime {

    private final HashMap<String, Variable> variables;
    private final HashMap<String, List<Function>> functions;

    public static SimpleRuntime simpleLang() {
        SimpleRuntime runtime = new SimpleRuntime();
        runtime.register(addNumbers());
        runtime.register(addStrings());
        return runtime;
    }

    private SimpleRuntime() {
        functions = new HashMap<>();
        variables = new HashMap<>();
    }

    @Override
    public void register(Variable v) {
        variables.put(v.name(), v);
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
            public Object run(Object... arguments) {
                String lhs = (String) arguments[0];
                String rhs = (String) arguments[1];
                return lhs + rhs;
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
            public Object run(Object... arguments) {
                Number lhs = (Number) arguments[0];
                Number rhs = (Number) arguments[1];
                return rhs.doubleValue() + lhs.doubleValue();
            }
        };
    }

    @Override
    public Object run(Program program) {
        Optional<Object> result = Optional.empty();
        for (int i = 0; i < program.codeElement().size(); ++i) {
            CodeElement codeElement = program.codeElement().get(i);
            result = switch (codeElement) {
                case Expression expression -> Optional.of(evaluateExpr(expression));
                case Statement statement -> {
                    runStatement(statement);
                    yield Optional.empty();
                }
            };
        }
        return result.orElseThrow(() -> new PeelException("Program did not contain any top level expression to return"));
    }

    private void runStatement(Statement statement) {
        switch (statement) {
            case Statement.Assignment(var name, var expression) -> variables.put(name, new Variable() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public Object value() {
                    return evaluateExpr(expression);
                }
            });
        }
    }

    private Object evaluateExpr(Expression expression) {
        return switch (expression) {
            case Expression.BinaryOperator operator -> evaluateOperator(
                    operator
            );
            case Expression.Literal(var literal) -> literal;
            case Expression.VariableName(var name) -> variables.get(name).value();
        };
    }

    private Object evaluateOperator(Expression.BinaryOperator operator) {
        List<Function> matchingName = functions.get(operator.operator());
        Object lhs = evaluateExpr(operator.lhs());
        Object rhs = evaluateExpr(operator.rhs());
        List<Function> list = matchingName.stream()
                .filter(f -> argumentsFit(f.arguments(), List.of(lhs, rhs)))
                .toList();
        Function f = requireOneFunction(
                list,
                getNoFunctionFoundException(operator, lhs, rhs),
                getMultipleFunctionsFoundException(operator)
        );
        return f.run(lhs, rhs);
    }

    private static NoFunctionFoundException getNoFunctionFoundException(Expression.BinaryOperator operator, Object lhs, Object rhs) {
        return new NoFunctionFoundException(operator.operator(), lhs, rhs);
    }

    private MultipleFunctionsFoundException getMultipleFunctionsFoundException(Expression.BinaryOperator operator) {
        return new MultipleFunctionsFoundException(operator.operator(), functions.size());
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

    private boolean argumentsFit(List<TypeDescriptor> arguments, List<Object> lhs) {
        boolean result = arguments.size() == lhs.size();
        if (result) {
            for (int i = 0; i < arguments.size() && result; ++i) {
                TypeDescriptor typeDescriptor = arguments.get(i);
                Object arg = lhs.get(i);
                result = matches(typeDescriptor, arg);
            }
        }
        return result;
    }

    private boolean matches(TypeDescriptor typeDescriptor, Object arg) {
        return switch (typeDescriptor) {
            case TypeDescriptor.Type(var t) -> t.isAssignableFrom(arg.getClass());
            case TypeDescriptor.ListOf(var _) -> List.class.isAssignableFrom(arg.getClass());
        };
    }
}