package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.*;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Bool;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.MultipleFunctionsFoundException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.PeelException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SimpleRuntime implements Runtime {


    private final Scope global;
    private final List<Scope> scopes;

    public static SimpleRuntime empty() {
        return new SimpleRuntime(
                Scope.empty(),
                new ArrayList<>()
        );
    }

    private SimpleRuntime(Scope global, List<Scope> scopes) {
        this.global = global;
        this.scopes = scopes;
    }

    @Override
    public void register(Variable v) {
        global.register(v);
    }

    @Override
    public void register(Function f) {
        global.register(f);
    }

    @Override
    public EvaluatedProgram run(Program program) {
        beginnScope();
        EvaluatedProgram evaluatedProgram = new EvaluatedProgram(program.programm().codeElements()
                .stream()
                .map(this::evaluateExpr)
                .toList()
        );
        endScope();
        return evaluatedProgram;

    }

    private EvaluatedExpression evaluateExpr(Expression expression) {
        return switch (expression) {
            case Expression.BinaryOperator operator -> evaluateOperator(
                    operator
            );
            case Expression.Literal(var value) -> new EvaluatedExpression.Literal(value);
            case Expression.VariableName(var name, var scopeOffset) ->
                    new EvaluatedExpression.VariableName(name, getVar(name, scopeOffset));
            case Expression.FunctionCall functionCall -> evaluateFunction(functionCall);
            case Expression.Assignment(var name, var expression1, int scopeOffset) ->
                    evaluateAssignment(name, expression1, scopeOffset);
            case Expression.Block(var expressions) -> evaluateBlock(expressions);
            case Expression.IfElseStatement(var elseIfs, var elseBlock) -> evaluateIfStatement(
                    elseIfs, elseBlock
            );
            case Expression.WhileLoop(var condition, var block) -> runLoop(condition, block);
            case Expression.UnaryPrefixOperator(var operator, var argument) -> evaluateUnary(operator, argument);
            case Expression.ForEachLoop(var varName, var listExpr, var block) ->
                    runForEachLoop(varName, listExpr, block);
            case Expression.ListLiteral(var list) -> evaluateListLiteral(list);
        };
    }

    private EvaluatedExpression.Assignment evaluateAssignment(
            String name,
            Expression expression1,
            int scopeOffset
    ) {
        if (scopeOffset == -1) {
            throw new PeelException("Can't assign to global variable");
        }
        EvaluatedExpression value = evaluateExpr(expression1);
        getScopeBy(scopeOffset).putVar(name, value.value());
        return new EvaluatedExpression.Assignment(
                name, value
        );
    }

    private int getScopeIndexBy(int scopeOffset) {
        return scopes.size() - 1 - scopeOffset;
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

    private PeelValue getVar(String name, int scopeOffset) {
        if (scopeOffset == -1) {
            return global.getVar(name);
        } else {
            return getScopeBy(scopeOffset).getVar(name);
        }
    }

    private Scope getScopeBy(int scopeOffset) {
        return scopes.get(getScopeIndexBy(scopeOffset));
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
            getScopeBy(0).putVar(varName, value);
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
        scopes.add(Scope.empty());
    }

    private void endScope() {
        scopes.removeLast();
    }

    private PeelValue.Collection.List requireList(EvaluatedExpression evaluatedExpression) {
        if (evaluatedExpression instanceof EvaluatedExpression.EvaluatedListLiteral(var _, var value)) {
            return value;
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

    private EvaluatedExpression evaluateFunction(Expression.FunctionCall functionCall) {
        List<EvaluatedExpression> arguments = functionCall.arguments().stream()
                .map(this::evaluateExpr)
                .toList();
        List<Function> matchingName = global.getFunction(functionCall.functionName());
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
        List<Function> matchingName = global.getFunction(operator.operator());
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