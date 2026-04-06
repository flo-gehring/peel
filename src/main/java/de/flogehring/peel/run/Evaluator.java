package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.*;
import de.flogehring.peel.run.exceptions.MultipleFunctionsFoundException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.PeelException;
import de.flogehring.peel.run.trace.SingleTraceExpressionRecorder;
import de.flogehring.peel.run.trace.TraceRecorder;
import de.flogehring.peel.run.trace.TraceSubRecorder;
import lombok.extern.java.Log;

import java.util.*;

import static de.flogehring.peel.core.values.PeelValue.Collection.peelList;

@Log
public class Evaluator {

    record BlockExecution(PeelValue value, TraceExpression.Block trace) {
    }

    private EvaluationEnvironment environment;
    private final OperatorResolver operatorResolver;

    public Evaluator(EvaluationEnvironment environment) {
        this.environment = environment;
        this.operatorResolver = new OperatorResolver();
    }

    PeelValue evaluate(Expression expression, TraceSubRecorder traceSubRecorder) {
        beginnScope();
        PeelValue result = evaluateExpr(expression, traceSubRecorder);
        endScope();
        return result;
    }

    private PeelValue evaluateExpr(Expression expression, TraceRecorder recorder) {
        return switch (expression) {
            case Expression.BinaryOperator operator -> evaluateOperator(operator, recorder);
            case Expression.Literal(var value) -> getLiteral(value, recorder);
            case Expression.VariableName varName -> resolveVariable(varName, recorder);
            case Expression.FunctionCall functionCall -> evaluateFunction(functionCall, recorder);
            case Expression.Assignment(var name, var assignedExpr, int scopeOffset) ->
                    evaluateAssignment(name, assignedExpr, scopeOffset, recorder);
            case Expression.Block(var expressions) -> evaluateBlock(expressions, recorder);
            case Expression.IfElseStatement(var elseIfs, var elseBlock) ->
                    evaluateIfStatement(elseIfs, elseBlock, recorder);
            case Expression.WhileLoop(var condition, var block) -> runLoop(condition, block, recorder);
            case Expression.UnaryPrefixOperator(var operator, var argument) ->
                    evaluateUnary(operator, argument, recorder);
            case Expression.ForEachLoop(var varName, var listExpr, var block) ->
                    runForEachLoop(varName, listExpr, block, recorder);
            case Expression.ListLiteral(var list) -> evaluateListLiteral(list, recorder);
            case Expression.MapLiteral(var entries) -> evaluateMapLiteral(entries, recorder);
            case Expression.Selector(var target, var selector) -> evaluateSelector(target, selector, recorder);
            case Expression.Return(var expr) -> {
                SingleTraceExpressionRecorder expressionRecorder = new SingleTraceExpressionRecorder();
                PeelValue value = evaluateExpr(expr, expressionRecorder);
                recorder.append(new TraceExpression.ReturnExpr(
                        toTraceValue(value),
                        expressionRecorder.expression()
                ));
                throw new ReturnValueFlow(value);
            }
            case Expression.FunctionDeclaration(var callable, var offset) ->
                    declareFunction(callable, offset, recorder);
        };
    }

    private PeelValue getLiteral(PeelValue value, TraceRecorder recorder) {
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
        recorder.append(new TraceExpression.Literal(toTraceValue(enriched)));
        return enriched;
    }

    private PeelValue resolveVariable(Expression.VariableName varName, TraceRecorder recorder) {
        PeelValue value = environment.isFunction(varName)
                ? FunctionReference.of(environment.getFunction(varName))
                : environment.getVar(varName);
        recorder.append(new TraceExpression.VariableName(varName.name(), toTraceValue(value)));
        return value;
    }

    private PeelValue declareFunction(PeelFunctionDefinition callable, int offset, TraceRecorder recorder) {
        environment.putFunction(new Expression.VariableName(callable.getName(), offset), getFunctionFrom(callable));
        recorder.append(new TraceExpression.Literal(toTraceValue(callable)));
        return callable;
    }

    private PeelValue evaluateAssignment(
            String name,
            Expression expression,
            int scopeOffset,
            TraceRecorder recorder
    ) {
        SingleTraceExpressionRecorder expressionRecorder = new SingleTraceExpressionRecorder();
        PeelValue value = evaluateExpr(expression, expressionRecorder);
        Expression.VariableName varName = new Expression.VariableName(name, scopeOffset);
        environment.put(varName, value);
        recorder.append(new TraceExpression.Assignment(name, expressionRecorder.expression(), toTraceValue(value)));
        return value;
    }

    private PeelValue evaluateIfStatement(
            List<Expression.IfElseStatement.ConditionalExecution> elseIfs,
            Optional<Expression> elseBlock,
            TraceRecorder recorder
    ) {
        List<TraceExpression> conditionTraces = new ArrayList<>();
        for (Expression.IfElseStatement.ConditionalExecution cond : elseIfs) {
            beginnScope();
            SingleTraceExpressionRecorder conditionRecorder = new SingleTraceExpressionRecorder();
            PeelValue conditionValue = evaluateExpr(cond.condition(), conditionRecorder);
            conditionTraces.add(conditionRecorder.traceExpression());
            endScope();
            if (requireBool(conditionValue)) {
                SingleTraceExpressionRecorder thenRecorder = new SingleTraceExpressionRecorder();
                PeelValue thenValue = evaluateExpr(cond.then(), thenRecorder);
                recorder.append(new TraceExpression.IfStatement(
                        conditionTraces,
                        thenRecorder.expression(),
                        toTraceValue(thenValue)
                ));
                return thenValue;
            }
        }
        if (elseBlock.isPresent()) {
            SingleTraceExpressionRecorder elseRecorder = new SingleTraceExpressionRecorder();
            PeelValue elseValue = evaluateExpr(elseBlock.get(), elseRecorder);
            recorder.append(new TraceExpression.IfStatement(
                    conditionTraces,
                    elseRecorder.expression(),
                    toTraceValue(elseValue)
            ));
            return elseValue;
        }
        recorder.append(new TraceExpression.IfStatement(
                conditionTraces,
                new TraceExpression.Block(List.of()),
                TraceValue.none()
        ));
        return None.NONE;
    }

    PeelValue evaluateBlock(List<Expression> expressions, TraceRecorder recorder) {
        beginnScope();
        TraceSubRecorder blockRecorder = new TraceSubRecorder();
        recorder.append(blockRecorder);
        PeelValue lastValue = None.NONE;
        for (Expression expression : expressions) {
            lastValue = evaluateExpr(expression, blockRecorder);
        }
        endScope();
        return lastValue;
    }

    private PeelValue evaluateListLiteral(List<Expression> list, TraceRecorder recorder) {
        List<PeelValue> values = new ArrayList<>();
        List<TraceExpression> elements = new ArrayList<>();
        for (Expression expression : list) {
            SingleTraceExpressionRecorder elementRecorder = new SingleTraceExpressionRecorder();
            PeelValue value = evaluateExpr(expression, elementRecorder);
            values.add(value);
            elements.add(elementRecorder.expression());
        }
        PeelValue.Collection.List out = peelList(values);
        recorder.append(new TraceExpression.ListLiteral(elements, toTraceValue(out)));
        return out;
    }

    private PeelValue evaluateMapLiteral(List<Expression.MapLiteral.Entry> entries, TraceRecorder recorder) {
        List<TraceExpression.MapLiteral.MapEntry> traceEntries = new ArrayList<>();
        LinkedHashMap<Primitives, PeelValue> map = new LinkedHashMap<>();
        for (Expression.MapLiteral.Entry entry : entries) {
            SingleTraceExpressionRecorder keyRecorder = new SingleTraceExpressionRecorder();
            PeelValue keyValue = evaluateExpr(entry.key(), keyRecorder);
            SingleTraceExpressionRecorder valueRecorder = new SingleTraceExpressionRecorder();
            PeelValue value = evaluateExpr(entry.value(), valueRecorder);
            map.put(requirePrimitive(keyValue), value);
            traceEntries.add(new TraceExpression.MapLiteral.MapEntry(keyRecorder.expression(), valueRecorder.expression()));
        }
        PeelValue.Collection.Map out = PeelValue.Collection.peelMap(Collections.unmodifiableMap(new LinkedHashMap<>(map)));
        recorder.append(new TraceExpression.MapLiteral(traceEntries, toTraceValue(out)));
        return out;
    }

    private PeelValue evaluateSelector(Expression target, Expression selector, TraceRecorder recorder) {
        SingleTraceExpressionRecorder targetRecorder = new SingleTraceExpressionRecorder();
        PeelValue evaluatedTarget = evaluateExpr(target, targetRecorder);
        SingleTraceExpressionRecorder selectorRecorder = new SingleTraceExpressionRecorder();
        PeelValue evaluatedSelector = evaluateExpr(selector, selectorRecorder);
        PeelValue selected = switch (evaluatedTarget) {
            case PeelValue.Collection.List(var list) -> selectFromList(list, evaluatedSelector);
            case PeelValue.Collection.Map(var map) -> selectFromMap(map, evaluatedSelector);
            default -> throw new PeelException(
                    "Selector target must be list or map, was {0}",
                    evaluatedTarget.getClass().getSimpleName()
            );
        };
        recorder.append(new TraceExpression.Selector(
                toTraceValue(selected),
                targetRecorder.expression(),
                selectorRecorder.expression()
        ));
        return selected;
    }

    private PeelValue selectFromList(List<PeelValue> list, PeelValue selector) {
        if (selector instanceof de.flogehring.peel.core.values.Number.Integer(Integer index)) {
            if (index < 0 || index >= list.size()) {
                throw new PeelException("List index out of bounds: {0}", index);
            }
            return list.get(index);
        }
        throw new PeelException("List selector must be Integer, was {0}", selector.getClass().getSimpleName());
    }

    private PeelValue selectFromMap(Map<Primitives, PeelValue> map, PeelValue selector) {
        if (selector instanceof Primitives primitive) {
            if (!map.containsKey(primitive)) {
                throw new PeelException("Map key not found: {0}", primitive);
            }
            return map.get(primitive);
        }
        throw new PeelException("Map selector must be primitive, was {0}", selector.getClass().getSimpleName());
    }

    private Primitives requirePrimitive(PeelValue value) {
        if (value instanceof Primitives primitive) {
            return primitive;
        }
        throw new PeelException("Map key must be primitive, was {0}", value.getClass().getSimpleName());
    }

    private PeelValue runForEachLoop(String varName, Expression listExpr, Expression.Block block, TraceRecorder recorder) {
        List<TraceExpression.ForEachLoop.Iteration> iterations = new ArrayList<>();
        PeelValue.Collection.List list = requireList(evaluateExpr(listExpr, new TraceSubRecorder()));
        PeelValue lastBodyValue = None.NONE;
        for (PeelValue value : list.list()) {
            beginnScope();
            environment.put(new Expression.VariableName(varName, 0), value);
            SingleTraceExpressionRecorder bodyRecorder = new SingleTraceExpressionRecorder();
            PeelValue bodyValue = evaluateExpr(block, bodyRecorder);
            lastBodyValue = bodyValue;
            iterations.add(new TraceExpression.ForEachLoop.Iteration(
                    toTraceValue(value),
                    (TraceExpression.Block) bodyRecorder.expression()
            ));
            endScope();
        }
        PeelValue out = iterations.isEmpty() ? None.NONE : lastBodyValue;
        recorder.append(new TraceExpression.ForEachLoop(iterations, toTraceValue(out)));
        return out;
    }

    private void beginnScope() {
        environment.enterScope();
    }

    private void endScope() {
        environment.exitScope();
    }

    private PeelValue.Collection.List requireList(PeelValue value) {
        if (value instanceof PeelValue.Collection.List list) {
            return list;
        }
        throw new PeelException("Expected list");
    }

    private PeelValue evaluateUnary(String operator, Expression argument, TraceRecorder recorder) {
        SingleTraceExpressionRecorder argumentRecorder = new SingleTraceExpressionRecorder();
        PeelValue expression = evaluateExpr(argument, argumentRecorder);
        List<OperatorDef> candidates = environment.getOperator(operator);
        PeelValue value = operatorResolver.resolveAndApply(operator, expression, candidates);
        recorder.append(new TraceExpression.UnaryPrefixOperator(operator, toTraceValue(value), argumentRecorder.expression()));
        return value;
    }

    private PeelValue runLoop(Expression condition, Expression.Block block, TraceRecorder recorder) {
        List<TraceExpression.WhileLoop.Iteration> iterations = new ArrayList<>();
        beginnScope();
        SingleTraceExpressionRecorder conditionRecorder = new SingleTraceExpressionRecorder();
        PeelValue conditionValue = evaluateExpr(condition, conditionRecorder);
        PeelValue lastBodyValue = None.NONE;
        while (requireBool(conditionValue)) {
            SingleTraceExpressionRecorder blockRecorder = new SingleTraceExpressionRecorder();
            lastBodyValue = evaluateExpr(block, blockRecorder);
            iterations.add(new TraceExpression.WhileLoop.Iteration(conditionRecorder.expression(), (TraceExpression.Block) blockRecorder.expression()));
            conditionRecorder = new SingleTraceExpressionRecorder();
            conditionValue = evaluateExpr(condition, conditionRecorder);
        }
        iterations.add(new TraceExpression.WhileLoop.Iteration(conditionRecorder.expression(), null));
        endScope();
        PeelValue out = iterations.size() == 1
                ? None.NONE
                : lastBodyValue;
        recorder.append(new TraceExpression.WhileLoop(iterations, toTraceValue(out)));
        return out;
    }

    private boolean requireBool(PeelValue value) {
        if (value instanceof Bool(var b)) {
            return b;
        } else {
            throw new PeelException("condition must be Bool, was {0}", value.getClass().getSimpleName());
        }
    }

    private PeelValue evaluateFunction(Expression.FunctionCall functionCall, TraceRecorder recorder) {
        List<PeelValue> arguments = new ArrayList<>();
        List<TraceExpression> argumentTraces = new ArrayList<>();
        for (Expression argument : functionCall.arguments()) {
            SingleTraceExpressionRecorder argumentRecorder = new SingleTraceExpressionRecorder();
            arguments.add(evaluateExpr(argument, argumentRecorder));
            argumentTraces.add(argumentRecorder.expression());
        }
        Function function = resolveFunctions(functionCall, arguments);
        PeelValue result;
        EvaluationEnvironment currentEnv = environment;
        environment = currentEnv.spawnChild();
        SingleTraceExpressionRecorder functionTrace = new SingleTraceExpressionRecorder();
        try {
            result = function.runWithTrace(functionTrace, arguments.toArray(new PeelValue[0]));
        } catch (ReturnValueFlow returnValueFlow) {
            result = returnValueFlow.getValue();
        }
        environment = currentEnv;
        recorder.append(new TraceExpression.FunctionCall(
                function.name(),
                toTraceValue(result),
                argumentTraces,
                Optional.of(new TraceExpression.FunctionExecutionTrace(
                        function.name(),
                        argumentTraces.stream().map(traceExpression -> new TraceExpression.ParameterBinding(
                                        "n", traceExpression
                                )
                        ).toList(),
                        (TraceExpression.Block) functionTrace.traceExpression()
                ))
        ));
        return result;
    }

    private Function resolveFunctions(Expression.FunctionCall functionCall, List<PeelValue> arguments) {
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
                 Expression.MapLiteral _,
                 Expression.Selector _,
                 Expression.UnaryPrefixOperator _,
                 Expression.WhileLoop _ -> {
                SingleTraceExpressionRecorder calleeRecorder = new SingleTraceExpressionRecorder();
                PeelValue value = evaluateExpr(callee, calleeRecorder);
                yield getFunctionFromPeelValue(value);
            }
            case Expression.ListLiteral _, Expression.Return _ ->
                    throw new PeelException("Can't call function on " + callee.getClass().getSimpleName());
            case Expression.FunctionDeclaration _ ->
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

    private PeelValue evaluateOperator(Expression.BinaryOperator operator, TraceRecorder recorder) {
        if (operator.operator().equals("&&")) {
            return evaluateLogicalAnd(operator.lhs(), operator.rhs(), recorder);
        }
        if (operator.operator().equals("||")) {
            return evaluateLogicalOr(operator.lhs(), operator.rhs(), recorder);
        }
        SingleTraceExpressionRecorder lhsRecorder = new SingleTraceExpressionRecorder();
        PeelValue lhs = evaluateExpr(operator.lhs(), lhsRecorder);
        SingleTraceExpressionRecorder rhsRecorder = new SingleTraceExpressionRecorder();
        PeelValue rhs = evaluateExpr(operator.rhs(), rhsRecorder);
        List<OperatorDef> candidates = environment.getOperator(operator.operator());
        PeelValue value = operatorResolver.resolveAndApply(operator.operator(), lhs, rhs, candidates);
        recorder.append(new TraceExpression.BinaryOperator(
                operator.operator(),
                toTraceValue(value),
                lhsRecorder.expression(),
                rhsRecorder.expression()
        ));
        return value;
    }

    private PeelValue evaluateLogicalAnd(Expression lhsExpression, Expression rhsExpression, TraceRecorder recorder) {
        SingleTraceExpressionRecorder lhsRecorder = new SingleTraceExpressionRecorder();
        PeelValue lhs = evaluateExpr(lhsExpression, lhsRecorder);
        if (!requireBool(lhs)) {
            recorder.append(new TraceExpression.LogicalBinaryOperator(
                    "&&",
                    new TraceValue.BoolValue(false),
                    lhsRecorder.expression(),
                    Optional.empty(),
                    true
            ));
            return PeelValue.bool(false);
        }
        SingleTraceExpressionRecorder rhsRecorder = new SingleTraceExpressionRecorder();
        PeelValue rhs = evaluateExpr(rhsExpression, rhsRecorder);
        PeelValue out = PeelValue.bool(requireBool(rhs));
        recorder.append(new TraceExpression.LogicalBinaryOperator(
                "&&",
                toTraceValue(out),
                lhsRecorder.expression(),
                Optional.of(rhsRecorder.expression()),
                false
        ));
        return out;
    }

    private PeelValue evaluateLogicalOr(Expression lhsExpression, Expression rhsExpression, TraceRecorder recorder) {
        SingleTraceExpressionRecorder lhsRecorder = new SingleTraceExpressionRecorder();
        PeelValue lhs = evaluateExpr(lhsExpression, lhsRecorder);
        if (requireBool(lhs)) {
            recorder.append(new TraceExpression.LogicalBinaryOperator(
                    "||",
                    new TraceValue.BoolValue(true),
                    lhsRecorder.expression(),
                    Optional.empty(),
                    true
            ));
            return PeelValue.bool(true);
        }
        SingleTraceExpressionRecorder rhsRecorder = new SingleTraceExpressionRecorder();
        PeelValue rhs = evaluateExpr(rhsExpression, rhsRecorder);
        PeelValue out = PeelValue.bool(requireBool(rhs));
        recorder.append(new TraceExpression.LogicalBinaryOperator(
                "||",
                toTraceValue(out),
                lhsRecorder.expression(),
                Optional.of(rhsRecorder.expression()),
                false
        ));
        return out;
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

    private TraceValue toTraceValue(PeelValue value) {
        return TraceValueMapper.fromPeelValue(value);
    }
}
