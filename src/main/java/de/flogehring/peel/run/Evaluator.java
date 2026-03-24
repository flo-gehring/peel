package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.*;
import de.flogehring.peel.run.exceptions.MultipleFunctionsFoundException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.PeelException;
import lombok.extern.java.Log;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.flogehring.peel.core.values.PeelValue.Collection.peelList;

@Log
public class Evaluator {

    private EvaluationEnvironment environment;

    public Evaluator(EvaluationEnvironment environment) {
        this.environment = environment;
    }

    EvaluatedExpression evaluate(Expression expression) {
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
            case Expression.Literal(var value) -> getLiteral(value);
            case Expression.VariableName varName -> resolveVariable(varName);
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
            case Expression.FunctionDeclaration(var callable, var offset) -> declareFunction(callable, offset);
        };
    }

    private EvaluatedExpression getLiteral(PeelValue value) {
        return switch (value) {
            case PeelCallable peelCallable -> new EvaluatedExpression.Closure(
                    new PeelClosure(peelCallable.getName(), peelCallable.getParameters(), peelCallable.getBody(), environment.copy()),
                    environment.copy()
            );
            case PeelValue.Collection _, Primitives _ -> new EvaluatedExpression.Literal(value);
        };
    }

    private EvaluatedExpression.VariableName resolveVariable(Expression.VariableName varName) {
        PeelValue var = environment.getVar(varName);
        System.out.println(MessageFormat.format("{0} <- {1} @ offset {2}", varName.name(), var, varName.scopeOffset()));
        return new EvaluatedExpression.VariableName(varName.name(), var);
    }

    private EvaluatedExpression declareFunction(PeelCallable callable, int offset) {
        environment.putFunction(getFunctionFrom(callable));
        return EvaluatedExpression.peelLiteral(callable);
    }

    private EvaluatedExpression.Assignment evaluateAssignment(
            String name,
            Expression expression,
            int scopeOffset
    ) {
        EvaluatedExpression value;
        value = evaluateExpr(expression);
        Expression.VariableName varName = new Expression.VariableName(name, scopeOffset);
        if (value instanceof EvaluatedExpression.Closure(PeelCallable callable, EvaluationEnvironment closedOver)) {
            environment.putFunction(varName, new PeelCodeFunction(callable, closedOver));
        } else {
            environment.put(varName, value.value());
        }
        return new EvaluatedExpression.Assignment(name, value);
    }

    private EvaluatedExpression.IfStatement evaluateIfStatement(List<Expression.IfElseStatement.ConditionalExecution> elseIfs, Optional<Expression> elseBlock) {
        for (Expression.IfElseStatement.ConditionalExecution cond : elseIfs) {
            beginnScope();
            EvaluatedExpression evaluatedCondition = evaluateExpr(cond.condition());
            endScope();
            if (requireBool(evaluatedCondition)) {
                EvaluatedExpression executedBlock = evaluateExpr(cond.then());
                return new EvaluatedExpression.IfStatement(
                        evaluatedCondition,
                        executedBlock
                );
            }
        }
        return elseBlock.map(
                block -> {
                    EvaluatedExpression evaluatedBlock = evaluateExpr(block);
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

    EvaluatedExpression.EvaluatedBlock evaluateBlock(List<Expression> expressions) {
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
        return new EvaluatedExpression.EvaluatedListLiteral(
                evaluated, peelList(evaluated.stream().map(EvaluatedExpression::value).toList()
        ));
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
        EvaluationEnvironment currentEnv = environment;
        environment = currentEnv.spawnChild();
        try {
            System.out.println(MessageFormat.format(
                    "{0}({1})", f.name(), String.join(", ", arguments.stream().map(EvaluatedExpression::toString).toList())
            ));
            value = f.run(arguments.toArray(new EvaluatedExpression[0]));
        } catch (ReturnValueFlow returnValueFlow) {
            System.out.println("Return " + returnValueFlow.getExpr().value());
            value = returnValueFlow.getExpr().value();
        }
        environment = currentEnv;
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
                if (environment.isLocalFunction(variableName)) {
                    yield environment.getLocalFunction(variableName);
                } else if (environment.isGlobalFunction(variableName)) {
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
                EvaluatedExpression e = evaluateExpr(functionCall.callee());
                if (e instanceof EvaluatedExpression.Closure(
                        PeelCallable callable, EvaluationEnvironment environment1
                )) {
                    yield List.of(new PeelCodeFunction(
                            callable,
                            environment1
                    ));
                }
                yield getFunctionFromPeelValue(e.value());
            }
            case Expression.ListLiteral _, Expression.Return _ ->
                    throw new PeelException("Can't call function on " + functionCall.callee().getClass().getSimpleName());
            case Expression.FunctionDeclaration _ -> // TODO On second thought, why not?
                    throw new PeelException("Can't call a function on a function declaration");
        };
    }

    private List<Function> getFunctionFromPeelValue(PeelValue val) {
        if (val instanceof PeelClosure closure) {
            return List.of(new PeelCodeFunction(
                    closure,
                    closure.getEvaluationEnvironment()
            ));
        }
        if (val instanceof PeelCallable callable) {
            return List.of(getFunctionFrom(callable));
        } else {
            throw new PeelException("Can't call Value of type " + val.getClass().getSimpleName());
        }
    }

    private Function getFunctionFrom(PeelCallable callable) {
        return new PeelCodeFunction(
                callable,
                environment.copy()
        );
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
