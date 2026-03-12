package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.Bool;
import de.flogehring.peel.core.values.PeelCallable;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.MultipleFunctionsFoundException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.PeelException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Evaluator {

    private final EvaluationEnvironment environment;
    private final Expression expression;

    public Evaluator(EvaluationEnvironment environment, Expression expression) {
        this.environment = environment;
        this.expression = expression;
    }

    EvaluatedExpression evaluate() {
        return getEvaluatedProgram();
    }

    private EvaluatedExpression getEvaluatedProgram() {
        beginnScope();
        EvaluatedExpression result = evaluateExpr(expression);
        endScope();
        return result;
    }

    private EvaluatedExpression evaluateExpr(Expression expression) {
        return switch (expression) {
            case Expression.BinaryOperator operator -> evaluateOperator(
                    operator
            );
            case Expression.Literal(var value) -> new EvaluatedExpression.Literal(value);
            case Expression.VariableName varName ->
                    new EvaluatedExpression.VariableName(varName.name(), environment.getVar(varName));
            case Expression.FunctionCall functionCall -> evaluateFunction(functionCall);
            case Expression.Assignment(var name, var assignedExpr, int scopeOffset) ->
                    evaluateAssignment(name, assignedExpr, scopeOffset);
            case Expression.Block(var expressions) -> evaluateBlock(expressions);
            case Expression.IfElseStatement(var elseIfs, var elseBlock) -> evaluateIfStatement(
                    elseIfs, elseBlock
            );
            case Expression.WhileLoop(var condition, var block) -> runLoop(condition, block);
            case Expression.UnaryPrefixOperator(var operator, var argument) -> evaluateUnary(operator, argument);
            case Expression.ForEachLoop(var varName, var listExpr, var block) ->
                    runForEachLoop(varName, listExpr, block);
            case Expression.ListLiteral(var list) -> evaluateListLiteral(list);
            case Expression.Return(var expr) -> throw new ReturnValueFlow(
                    evaluateExpr(expr)
            );
        };
    }

    private EvaluatedExpression.Assignment evaluateAssignment(
            String name,
            Expression expression,
            int scopeOffset
    ) {
        EvaluatedExpression value;
        value = evaluateExpr(expression);
        environment.put(
                new Expression.VariableName(name, scopeOffset),
                value.value()
        );
        return new EvaluatedExpression.Assignment(
                name, value
        );
    }

    private EvaluatedExpression.IfStatement evaluateIfStatement(List<Expression.IfElseStatement.ConditionalExecution> elseIfs, Optional<Expression> elseBlock) {
        for (Expression.IfElseStatement.ConditionalExecution cond : elseIfs) {
            beginnScope();
            EvaluatedExpression evaluatedCondition = evaluateExpr(cond.condition());
            endScope();
            if (requireBool(evaluatedCondition)) {
                beginnScope();
                EvaluatedExpression executedBlock = evaluateExpr(cond.then());
                endScope();
                return new EvaluatedExpression.IfStatement(
                        evaluatedCondition,
                        executedBlock
                );
            }
        }
        return elseBlock.map(
                block -> {
                    beginnScope();
                    EvaluatedExpression evaluatedBlock = evaluateExpr(block);
                    endScope();
                    return new EvaluatedExpression.IfStatement(
                            EvaluatedExpression.EvaluatedBlock.empty(),
                            evaluatedBlock
                    );
                }
        ).orElseGet(() -> new EvaluatedExpression.IfStatement(
                EvaluatedExpression.EvaluatedBlock.empty(),
                EvaluatedExpression.EvaluatedBlock.empty()
        ));
    }

    private EvaluatedExpression.EvaluatedBlock evaluateBlock(List<Expression> expressions) {
        beginnScope();
        List<EvaluatedExpression> blockStatements = expressions
                .stream()
                .map(this::evaluateExpr)
                .toList();
        endScope();
        return new EvaluatedExpression.EvaluatedBlock(blockStatements);
    }

    private EvaluatedExpression evaluateListLiteral(List<Expression> list) {
        List<EvaluatedExpression> evaluated = list.stream().map(this::evaluateExpr).toList();
        return new EvaluatedExpression.EvaluatedListLiteral(evaluated,
                new PeelValue.Collection.List(
                        evaluated.stream().map(EvaluatedExpression::value).toList()
                )
        );
    }

    private EvaluatedExpression runForEachLoop(String varName, Expression listExpr, Expression.Block block) {
        List<EvaluatedExpression.ForEachLoop.Iteration> iterations = new ArrayList<>();
        PeelValue.Collection.List list = requireList(evaluateExpr(listExpr));
        for (PeelValue value : list.list()) {
            beginnScope();
            environment.put(
                    new Expression.VariableName(varName, 0),
                    value
            );
            iterations.add(
                    new EvaluatedExpression.ForEachLoop.Iteration(
                            value,
                            (EvaluatedExpression.EvaluatedBlock) evaluateExpr(block)
                    )
            );
            endScope();
        }
        return new EvaluatedExpression.ForEachLoop(iterations);
    }

    private void beginnScope() {
        environment.enterScope();
    }

    private void endScope() {
        environment.exitScope();
    }

    private PeelValue.Collection.List requireList(EvaluatedExpression evaluatedExpression) {
        if (evaluatedExpression.value() instanceof PeelValue.Collection.List list) {
            return list;
        }
        throw new PeelException("Expected list");
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
        beginnScope();
        EvaluatedExpression evaluatedCondition = evaluateExpr(condition);
        while (requireBool(evaluatedCondition)) {
            EvaluatedExpression.EvaluatedBlock evaluatedBlock = (EvaluatedExpression.EvaluatedBlock) evaluateExpr(block);
            iterations.add(new EvaluatedExpression.WhileLoop.Iteration(evaluatedCondition, Optional.of(evaluatedBlock)));
            evaluatedCondition = evaluateExpr(condition);
        }
        iterations.add(new EvaluatedExpression.WhileLoop.Iteration(evaluatedCondition, Optional.empty()));
        endScope();
        return new EvaluatedExpression.WhileLoop(iterations);
    }

    private boolean requireBool(EvaluatedExpression evaluatedExpression) {
        if (evaluatedExpression.value() instanceof Bool(var b)) {
            return b;
        } else {
            throw new PeelException("condition must be Bool, was {0}", evaluatedExpression.value().getClass().getSimpleName());
        }
    }

    private EvaluatedExpression evaluateFunction(
            Expression.FunctionCall functionCall
    ) {
        List<EvaluatedExpression> arguments = functionCall.arguments().stream()
                .map(this::evaluateExpr)
                .toList();
        Function f = resolveFunctions(functionCall, arguments);
        PeelValue value;
        try {
            value = f.run(arguments.toArray(new EvaluatedExpression[0]));
        } catch (ReturnValueFlow returnValueFlow) {
            value = returnValueFlow.getExpr().value();
        }
        return new EvaluatedExpression.FunctionCall(f.name(), value, arguments);
    }

    private Function resolveFunctions(Expression.FunctionCall functionCall, List<EvaluatedExpression> arguments) {
        List<Function> matchingFunctions = getMatchingFunctions(functionCall);
        int argumentLength = arguments.size();
        List<Function> list = matchingFunctions.stream()
                .filter(f -> f.arity() == argumentLength)
                .toList();
        return requireOneFunction(
                list,
                getNoFunctionFoundException(functionCall.callee().toString(), arguments.toArray()),
                getMultipleFunctionsFoundException("", list)
        );
    }

    private List<Function> getMatchingFunctions(Expression.FunctionCall functionCall) {
        return switch (functionCall.callee()) {
            case Expression.VariableName variableName -> {
                if (environment.isFunction(variableName)) {
                    yield environment.getFunction(variableName);
                } else if (environment.isVar(variableName)) {
                    PeelValue value = environment.getVar(variableName);
                    yield getFunctionFromPeelValue(value);
                } else {
                    throw new PeelException("No Function var Variable Name" + variableName.name());
                }
            }
            case Expression.Assignment _,
                 Expression.BinaryOperator _, Expression.Block _,
                 Expression.ForEachLoop _,
                 Expression.FunctionCall _,
                 Expression.IfElseStatement _,
                 Expression.Literal _,
                 Expression.UnaryPrefixOperator _,
                 Expression.WhileLoop _ -> {
                PeelValue val = evaluateExpr(functionCall.callee()).value();
                yield getFunctionFromPeelValue(val);
            }
            case Expression.ListLiteral _, Expression.Return _ ->
                    throw new PeelException("Can't call function on " + functionCall.callee().getClass().getSimpleName());
        };
    }

    private List<Function> getFunctionFromPeelValue(PeelValue val) {
        if (val instanceof PeelCallable callable) {
            return List.of(getFunctionFrom(callable));
        } else {
            throw new PeelException("Can't call Value of type " + val.getClass().getSimpleName());
        }
    }

    private Function getFunctionFrom(PeelCallable callable) {
        return new Function() {
            @Override
            public String name() {
                return callable.getName();
            }

            @Override
            public int arity() {
                return callable.getParameters().size();
            }

            @Override
            public PeelValue run(EvaluatedExpression... arguments) {
                beginnScope();
                for (int i = 0; i < callable.getParameters().size(); ++i) {
                    environment.put(
                            new Expression.VariableName(callable.getParameters().get(i), 0),
                            arguments[i].value()
                    );
                }
                EvaluatedExpression.EvaluatedBlock evaluatedBlock = evaluateBlock(callable.getBody().codeElements());
                endScope();
                return evaluatedBlock.value();
            }
        };
    }

    private EvaluatedExpression evaluateOperator(Expression.BinaryOperator operator) {
        // TODO add Special Support for Operators
        List<Function> matchingName = environment.getFunction(operator.operator());
        List<Expression> parameters = List.of(operator.lhs(), operator.rhs());
        Function f = requireOneFunction(
                matchingName,
                getNoFunctionFoundException(operator.operator(), parameters),
                getMultipleFunctionsFoundException(operator.operator(), matchingName)
        );
        EvaluatedExpression lhs = evaluateExpr(operator.lhs());
        EvaluatedExpression rhs = evaluateExpr(operator.rhs());
        return new EvaluatedExpression.FunctionCall(
                operator.operator(),
                f.run(lhs, rhs),
                List.of(lhs, rhs)
        );
    }

    private static NoFunctionFoundException getNoFunctionFoundException(String operator, Object... arguments) {
        return new NoFunctionFoundException(operator, arguments);
    }

    private MultipleFunctionsFoundException getMultipleFunctionsFoundException(
            String operator, List<Function> list
    ) {
        return new MultipleFunctionsFoundException(operator, list.size());
    }

    private Function requireOneFunction(
            List<Function> list,
            NoFunctionFoundException e,
            MultipleFunctionsFoundException multipleFunctionsFoundException
    ) {
        if (list.isEmpty()) {
            throw e;
        } else if (list.size() > 1) {
            throw multipleFunctionsFoundException;
        } else {
            return list.getFirst();
        }
    }
}
