package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.*;
import de.flogehring.peel.run.exceptions.MultipleFunctionsFoundException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.PeelException;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.flogehring.peel.core.values.PeelValue.Collection.peelList;

@Log
public class Evaluator {

    private EvaluationEnvironment environment;
    private final OperatorResolver operatorResolver;

    public Evaluator(EvaluationEnvironment environment) {
        this.environment = environment;
        this.operatorResolver = new OperatorResolver();
    }

    EvaluatedExpression evaluate(Expression expression) {
        beginnScope();
        EvaluatedExpression result = evaluateExpr(expression);
        endScope();
        return result;
    }

    private EvaluatedExpression evaluateExpr(Expression expression) {
        return switch (expression) {
            case Expression.BinaryOperator operator -> evaluateOperator(operator);
            case Expression.Literal(var value) -> getLiteral(value);
            case Expression.VariableName varName -> resolveVariable(varName);
            case Expression.FunctionCall functionCall -> evaluateFunction(functionCall);
            case Expression.Assignment(var name, var assignedExpr, int scopeOffset) ->
                    evaluateAssignment(name, assignedExpr, scopeOffset);
            case Expression.Block(var expressions) -> evaluateBlock(expressions);
            case Expression.IfElseStatement(var elseIfs, var elseBlock) -> evaluateIfStatement(elseIfs, elseBlock);
            case Expression.WhileLoop(var condition, var block) -> runLoop(condition, block);
            case Expression.UnaryPrefixOperator(var operator, var argument) -> evaluateUnary(operator, argument);
            case Expression.ForEachLoop(var varName, var listExpr, var block) ->
                    runForEachLoop(varName, listExpr, block);
            case Expression.ListLiteral(var list) -> evaluateListLiteral(list);
            case Expression.Return(var expr) -> throw new ReturnValueFlow(evaluateExpr(expr));
            case Expression.FunctionDeclaration(var callable, var offset) -> declareFunction(callable, offset);
        };
    }

    private EvaluatedExpression getLiteral(PeelValue value) {
        PeelValue enriched = switch (value) {
            case PeelCallable callable -> switch (callable) {
                case FunctionReference _ -> value;
                case PeelFunctionDefinition functionDefinition -> switch (functionDefinition) {
                    case PeelClosure closure ->
                            new PeelClosure(closure.getName(), closure.getParameters(), closure.getBody(), closure.getEvaluationEnvironment().copy());
                    case PeelFunctionDefinition def ->
                            new PeelClosure(def.getName(), def.getParameters(), def.getBody(), environment.copy());
                };
            };
            case PeelValue.Collection _, Primitives _ -> value;
        };
        return EvaluatedExpression.peelLiteral(enriched);
    }

    private EvaluatedExpression.VariableName resolveVariable(Expression.VariableName varName) {
        if (environment.isFunction(varName)) {
            return new EvaluatedExpression.VariableName(varName.name(), FunctionReference.of(environment.getFunction(varName)));
        }
        PeelValue var = environment.getVar(varName);
        return new EvaluatedExpression.VariableName(varName.name(), var);
    }

    private EvaluatedExpression declareFunction(PeelFunctionDefinition callable, int offset) {
        environment.putFunction(new Expression.VariableName(callable.getName(), offset), getFunctionFrom(callable));
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
        environment.put(varName, value.value());
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
        EvaluatedExpression expression = evaluateExpr(argument);
        List<OperatorDef> candidates = environment.getOperator(operator);
        PeelValue value = operatorResolver.resolveAndApply(operator, expression.value(), candidates);
        return new EvaluatedExpression.UnaryPrefixOperator(
                operator,
                value,
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
        Function f = resolveFunctions(functionCall, arguments);
        PeelValue value;
        EvaluationEnvironment currentEnv = environment;
        environment = currentEnv.spawnChild();
        try {
            value = f.run(arguments.toArray(new EvaluatedExpression[0]));
        } catch (ReturnValueFlow returnValueFlow) {
            value = returnValueFlow.getExpr().value();
        }
        environment = currentEnv;
        return new EvaluatedExpression.FunctionCall(f.name(), value, arguments);
    }

    private Function resolveFunctions(Expression.FunctionCall functionCall, List<EvaluatedExpression> arguments) {
        List<Function> matchingFunctions = getMatchingFunctions(functionCall.callee());
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

    private List<Function> getMatchingFunctions(Expression callee) {
        return switch (callee) {
            case Expression.VariableName variableName -> {
                if (environment.isFunction(variableName)) {
                    yield environment.getFunction(variableName);
                } else {
                    yield getFunctionFromPeelValue(environment.getVar(variableName));
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
                EvaluatedExpression e = evaluateExpr(callee);
                yield getFunctionFromPeelValue(e.value());
            }
            case Expression.ListLiteral _, Expression.Return _ ->
                    throw new PeelException("Can't call function on " + callee.getClass().getSimpleName());
            case Expression.FunctionDeclaration _ -> // TODO On second thought, why not?
                    throw new PeelException("Can't call a function on a function declaration");
        };
    }

    private List<Function> getFunctionFromPeelValue(PeelValue val) {
        return switch (val) {
            case PeelClosure closure -> List.of(new PeelCodeFunction(
                    closure,
                    closure.getEvaluationEnvironment().copy()
            ));
            case PeelFunctionDefinition callable -> List.of(getFunctionFrom(callable));
            case FunctionReference functionReference -> functionReference.getFunctions();
            default -> throw new PeelException("Can't call Value of type " + val.getClass().getSimpleName());
        };
    }

    private Function getFunctionFrom(PeelFunctionDefinition callable) {
        return new PeelCodeFunction(callable, environment.copy());
    }

    private EvaluatedExpression evaluateOperator(Expression.BinaryOperator operator) {
        EvaluatedExpression lhs = evaluateExpr(operator.lhs());
        EvaluatedExpression rhs = evaluateExpr(operator.rhs());
        List<OperatorDef> candidates = environment.getOperator(operator.operator());
        PeelValue value = operatorResolver.resolveAndApply(operator.operator(), lhs.value(), rhs.value(), candidates);
        return new EvaluatedExpression.FunctionCall(
                operator.operator(),
                value,
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
