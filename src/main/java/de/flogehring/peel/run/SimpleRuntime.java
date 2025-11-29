package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.*;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Bool;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class SimpleRuntime implements Runtime {

    private final HashMap<String, EvaluatedExpression> variables;
    private final HashMap<String, List<Function>> functions;

    public static SimpleRuntime empty() {
        return new SimpleRuntime();
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

    @Override
    public EvaluatedProgram run(Program program) {
        return new EvaluatedProgram(program.programm().codeElements()
                .stream()
                .map(this::evaluateExpr)
                .toList()
        );

    }

    private EvaluatedExpression evaluateExpr(Expression expression) {
        return switch (expression) {
            case Expression.BinaryOperator operator -> evaluateOperator(
                    operator
            );
            case Expression.Literal(var value) -> new EvaluatedExpression.Literal(value);
            case Expression.VariableName(var name) -> variables.get(name);
            case Expression.FunctionCall functionCall -> evaluateFunction(functionCall);
            case Expression.Assignment(var name, var expression1) -> {
                EvaluatedExpression value = evaluateExpr(expression1);
                variables.put(name, value);
                yield new EvaluatedExpression.Assignment(
                        name, value
                );
            }
            case Expression.Block(var expressions) -> new EvaluatedExpression.EvaluatedBlock(
                    expressions
                            .stream()
                            .map(this::evaluateExpr)
                            .toList()
            );
            case Expression.IfElseStatement(var condition, var ifBlock, var elseBlock) -> {
                var evaluatedCondition = evaluateExpr(condition);
                if (evaluatedCondition.value() instanceof Bool(var c)) {
                    EvaluatedExpression evaluatedBlock = evaluateExpr(c ? ifBlock : elseBlock);
                    yield new EvaluatedExpression.IfStatement(
                            evaluatedCondition,
                            (EvaluatedExpression.EvaluatedBlock) evaluatedBlock
                    );
                } else {
                    throw new IllegalArgumentException(
                            MessageFormat.format(
                                    "condition must be Bool, was {0}",
                                    evaluatedCondition.value().getClass().getSimpleName()
                            )
                    );
                }
            }
            case Expression.IfStatement(var condition, var block) -> {
                var evaluatedCondition = evaluateExpr(condition);
                if (evaluatedCondition.value() instanceof Bool(var c)) {
                    EvaluatedExpression evaluatedBlock = c
                            ? evaluateExpr(block)
                            : new EvaluatedExpression.EvaluatedBlock(List.of());
                    yield new EvaluatedExpression.IfStatement(
                            evaluatedCondition,
                            (EvaluatedExpression.EvaluatedBlock) evaluatedBlock
                    );
                } else {
                    throw new IllegalArgumentException(
                            MessageFormat.format(
                                    "condition must be Bool, was {0}",
                                    evaluatedCondition.value().getClass().getSimpleName()
                            )
                    );
                }
            }
        };
    }

    private EvaluatedExpression evaluateFunction(Expression.FunctionCall functionCall) {
        List<EvaluatedExpression> arguments = functionCall.arguments().stream()
                .map(this::evaluateExpr)
                .toList();
        List<Function> matchingName = functions.get(functionCall.functionName());
        int argumentLength = arguments.size();
        List<Function> list = matchingName.stream()
                .filter(f -> f.arity() == argumentLength)
                .toList();
        Function f = requireOneFunction(
                list,
                getNoFunctionFoundException(functionCall.functionName(), arguments.toArray()),
                getMultipleFunctionsFoundException(functionCall.functionName(), list)
        );
        return f.run(arguments.toArray(new EvaluatedExpression[0]));
    }

    private EvaluatedExpression evaluateOperator(Expression.BinaryOperator operator) {
        // TODO add Special Support for Operators
        List<Function> matchingName = functions.get(operator.operator());
        List<Expression> parameters = List.of(operator.lhs(), operator.rhs());
        Function f = requireOneFunction(
                matchingName,
                getNoFunctionFoundException(operator.operator(), parameters),
                getMultipleFunctionsFoundException(operator.operator(), matchingName)
        );
        return f.run(evaluateExpr(operator.lhs()), evaluateExpr(operator.rhs()));
    }

    private static NoFunctionFoundException getNoFunctionFoundException(String operator, Object... arguments) {
        return new NoFunctionFoundException(operator, arguments);
    }

    private MultipleFunctionsFoundException getMultipleFunctionsFoundException(
            String operator, List<Function> list
    ) {
        return new MultipleFunctionsFoundException(operator, list.size());
    }

    private Function requireOneFunction(List<Function> list, NoFunctionFoundException
            e, MultipleFunctionsFoundException multipleFunctionsFoundException) {
        if (list.isEmpty()) {
            throw e;
        } else if (list.size() > 1) {
            throw multipleFunctionsFoundException;
        } else {
            return list.getFirst();
        }
    }
}