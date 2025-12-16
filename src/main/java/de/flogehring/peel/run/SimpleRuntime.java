package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.*;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Bool;
import de.flogehring.peel.core.values.PeelValue;

import java.util.*;
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
            case Expression.IfElseStatement(var elseIfs, var elseBlock) -> {

                EvaluatedExpression result;
                for (Expression.IfElseStatement.ConditionalExecution cond : elseIfs) {
                    EvaluatedExpression evaluatedCondition = evaluateExpr(cond.condition());
                    if (requireBool(evaluatedCondition)) {
                        yield new EvaluatedExpression.IfStatement(
                                evaluatedCondition,
                                evaluateExpr(cond.then())
                        );
                    }
                }
                yield elseBlock.map(
                        block -> new EvaluatedExpression.IfStatement(
                                EvaluatedExpression.EvaluatedBlock.empty(),
                                evaluateExpr(block)
                        )
                ).orElseGet(() -> new EvaluatedExpression.IfStatement(
                        EvaluatedExpression.EvaluatedBlock.empty(),
                        EvaluatedExpression.EvaluatedBlock.empty()
                ));
            }
            case Expression.Loop(var condition, var block) -> runLoop(condition, block);
            case Expression.UnaryPrefixOperator(var operator, var argument) -> evaluateUnary(operator, argument);
        };
    }

    private EvaluatedExpression evaluateUnary(String operator, Expression argument) {
        if (!Objects.equals(operator, "!")) {
            throw new PeelException("Currently only the Unary-Not is supported");
        }
        EvaluatedExpression expression = evaluateExpr(argument);
        return new EvaluatedExpression.UnaryPrefixOperator(
                operator,
                PeelValue.bool(!requireBool(expression)),
                expression
        );
    }

    private EvaluatedExpression runLoop(Expression condition, Expression.Block block) {
        List<EvaluatedExpression.WhileLoop.Iteration> iterations = new ArrayList<>();
        EvaluatedExpression evaluatedCondition = evaluateExpr(condition);
        while (requireBool(evaluatedCondition)) {
            EvaluatedExpression.EvaluatedBlock evaluatedBlock = (EvaluatedExpression.EvaluatedBlock) evaluateExpr(block);
            iterations.add(new EvaluatedExpression.WhileLoop.Iteration(evaluatedCondition, Optional.of(evaluatedBlock)));
            evaluatedCondition = evaluateExpr(condition);
        }
        iterations.add(new EvaluatedExpression.WhileLoop.Iteration(evaluatedCondition, Optional.empty()));
        return new EvaluatedExpression.WhileLoop(iterations);
    }


    private boolean requireBool(EvaluatedExpression evaluatedExpression) {
        if (evaluatedExpression.value() instanceof Bool(var b)) {
            return b;
        } else {
            throw new PeelException("condition must be Bool, was {0}", evaluatedExpression.value().getClass().getSimpleName());
        }
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