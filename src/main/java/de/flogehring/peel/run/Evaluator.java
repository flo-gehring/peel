package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.*;
import de.flogehring.peel.run.exceptions.MultipleFunctionsFoundException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.PeelException;
import de.flogehring.peel.run.trace.*;

import java.util.*;

import static de.flogehring.peel.core.values.PeelValue.Collection.peelList;

public class Evaluator {

    private EvaluationEnvironment environment;
    private final OperatorResolver operatorResolver;

    public Evaluator(EvaluationEnvironment environment) {
        this.environment = environment;
        this.operatorResolver = new OperatorResolver();
    }

    PeelValue evaluate(Expression expression, ExpressionRecorder expressionRecorder) {
        beginnScope();
        PeelValue result = evaluateExpr(expression, expressionRecorder);
        endScope();
        return result;
    }

    private PeelValue evaluateExpr(Expression expression, ExpressionRecorder recorder) {
        return switch (expression) {
            case Expression.BinaryOperator operator -> evaluateOperator(operator, recorder.recordBinary());
            case Expression.Literal(var value) -> getLiteral(value, recorder.literalRecorder());
            case Expression.VariableName varName -> resolveVariable(varName, recorder.recordVarName());
            case Expression.FunctionCall functionCall -> evaluateFunction(functionCall, recorder.recordFunctionCall());
            case Expression.Assignment(var name, var assignedExpr, int scopeOffset) ->
                    evaluateAssignment(name, assignedExpr, scopeOffset, recorder.recordAssignment());
            case Expression.Block(var expressions) -> evaluateBlock(expressions, recorder.recordBlock());
            case Expression.IfElseStatement(var elseIfs, var elseBlock) ->
                    evaluateIfStatement(elseIfs, elseBlock, recorder.recordElseIf());
            case Expression.WhileLoop(var condition, var block) ->
                    runLoop(condition, block, recorder.recordWhileLoop());
            case Expression.UnaryPrefixOperator(var operator, var argument) ->
                    evaluateUnary(operator, argument, recorder.recordUnary());
            case Expression.ForEachLoop(var varName, var listExpr, var block) ->
                    runForEachLoop(varName, listExpr, block, recorder.recordForEachLoop());
            case Expression.ListLiteral(var list) -> evaluateListLiteral(
                    list, recorder.recordListLiteral());
            case Expression.MapLiteral(var entries) -> evaluateMapLiteral(entries, recorder.recordMapLiteral());
            case Expression.Selector(var target, var selector) ->
                    evaluateSelector(target, selector, recorder.recordSelector());
            case Expression.Return(var expr) -> evaluateReturn(expr, recorder.recordReturn());
            case Expression.FunctionDeclaration(var callable, var offset) ->
                    declareFunction(callable, offset, recorder.recordFunctionDeclaration());
        };
    }

    private PeelValue evaluateReturn(Expression expr, ReturnExpressionRecorder recorder) {
        PeelValue value = evaluateExpr(expr, recorder.getExpressionRecorder());
        throw new ReturnValueFlow(value);
    }

    private PeelValue getLiteral(PeelValue value, LiteralRecorder recorder) {
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
        recorder.recordLiteral(enriched);
        return enriched;
    }

    private PeelValue resolveVariable(Expression.VariableName varName, VariableNameRecorder recorder) {
        PeelValue value = environment.isFunction(varName)
                ? FunctionReference.of(varName.name(), environment.getFunction(varName))
                : environment.getVar(varName);
        recorder.recordVarName(varName.name());
        recorder.recordValue(value);
        return value;
    }

    private PeelValue declareFunction(PeelFunctionDefinition callable, int offset, FunctionDeclarationRecorder functionDeclarationRecorder) {
        functionDeclarationRecorder.recordCallable(callable.getName(), callable.getParameters());
        environment.putFunction(new Expression.VariableName(callable.getName(), offset), getFunctionFrom(callable));
        return callable;
    }

    private PeelValue evaluateAssignment(
            String name,
            Expression expression,
            int scopeOffset,
            AssignmentRecorder assignmentRecorder
    ) {
        assignmentRecorder.setVarName(name);
        PeelValue value = evaluateExpr(expression, assignmentRecorder.recordAssignmentExpression());
        Expression.VariableName varName = new Expression.VariableName(name, scopeOffset);
        environment.put(varName, value);
        return value;
    }

    private PeelValue evaluateIfStatement(
            List<Expression.IfElseStatement.ConditionalExecution> elseIfs,
            Optional<Expression> elseBlock,
            IfElseRecorder recorder
    ) {
        for (Expression.IfElseStatement.ConditionalExecution cond : elseIfs) {
            beginnScope();
            PeelValue conditionValue = evaluateExpr(cond.condition(), recorder.nextCondition());
            endScope();
            if (requireBool(conditionValue)) {
                return evaluateExpr(cond.then(), recorder.recordBlock());
            }
        }
        if (elseBlock.isPresent()) {
            return evaluateExpr(elseBlock.get(), recorder.recordElse());
        }
        return None.NONE;
    }

    PeelValue evaluateBlock(List<Expression> expressions, BlockTraceRecorder recorder) {
        beginnScope();
        PeelValue lastValue = None.NONE;
        for (Expression expression : expressions) {
            lastValue = evaluateExpr(expression, recorder.nextRecorder());
        }
        endScope();
        return lastValue;
    }

    private PeelValue evaluateListLiteral(List<Expression> list, ListLiteralRecorder recorder) {
        List<PeelValue> values = new ArrayList<>();
        for (Expression expression : list) {
            PeelValue value = evaluateExpr(expression, recorder.subElementRecorder());
            values.add(value);
        }
        return peelList(values);
    }

    private PeelValue evaluateMapLiteral(
            List<Expression.MapLiteral.Entry> entries,
            MapLiteralRecorder mapLiteralRecorder
    ) {
        LinkedHashMap<Primitives, PeelValue> map = new LinkedHashMap<>();
        for (Expression.MapLiteral.Entry entry : entries) {
            MapLiteralRecorder.KeyValueRecorder recorder = mapLiteralRecorder.nextKeyValueRecorder();
            PeelValue keyValue = evaluateExpr(entry.key(), recorder.keyRecorder());
            PeelValue value = evaluateExpr(entry.value(), recorder.valueRecorder());
            map.put(requirePrimitive(keyValue), value);
        }
        return PeelValue.Collection.peelMap(Collections.unmodifiableMap(new LinkedHashMap<>(map)));
    }

    private PeelValue evaluateSelector(Expression target, Expression selector, SelectorRecorder recorder) {
        PeelValue evaluatedTarget = evaluateExpr(target, recorder.targetRecorder());
        PeelValue evaluatedSelector = evaluateExpr(selector, recorder.selectorRecorder());
        PeelValue peelValue = switch (evaluatedTarget) {
            case PeelValue.Collection.List(var list) -> selectFromList(list, evaluatedSelector);
            case PeelValue.Collection.Map(var map) -> selectFromMap(map, evaluatedSelector);
            default -> throw new PeelException(
                    "Selector target must be list or map, was {0}",
                    evaluatedTarget.getClass().getSimpleName()
            );
        };
        recorder.recordValue(peelValue);
        return peelValue;
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

    private PeelValue runForEachLoop(
            String varName,
            Expression listExpr,
            Expression.Block block,
            ForEachRecorder recorder
    ) {
        recorder.recordVariableName(varName);
        PeelValue.Collection.List list = requireList(evaluateExpr(listExpr, recorder.listRecorder()));
        PeelValue lastBodyValue = None.NONE;
        for (PeelValue value : list.list()) {
            beginnScope();
            environment.put(new Expression.VariableName(varName, 0), value);
            lastBodyValue = evaluateExpr(block, recorder.nextLoop(value));
            endScope();
        }
        return lastBodyValue;
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

    private PeelValue evaluateUnary(String operator, Expression argument, UnaryRecorder recorder) {
        recorder.recordOperator(operator);
        PeelValue expression = evaluateExpr(argument, recorder.recordOperand());
        List<OperatorDef> candidates = environment.getOperator(operator);
        PeelValue value = operatorResolver.resolveAndApply(operator, expression, candidates);
        recorder.recordValue(value);
        return value;
    }

    private PeelValue runLoop(Expression condition, Expression.Block block, WhileLoopRecorder recorder) {
        beginnScope();
        PeelValue conditionValue = evaluateExpr(condition, recorder.nextCondition());
        PeelValue lastBodyValue = None.NONE;
        while (requireBool(conditionValue)) {
            lastBodyValue = evaluateExpr(block, recorder.nextBody());
            conditionValue = evaluateExpr(condition, recorder.nextCondition());
        }
        endScope();
        return lastBodyValue;
    }

    private boolean requireBool(PeelValue value) {
        if (value instanceof Bool(var b)) {
            return b;
        } else {
            throw new PeelException("condition must be Bool, was {0}", value.getClass().getSimpleName());
        }
    }

    private PeelValue evaluateFunction(
            Expression.FunctionCall functionCall,
            FunctionCallRecorder recorder
    ) {
        List<PeelValue> arguments = new ArrayList<>();
        for (Expression argument : functionCall.arguments()) {
            arguments.add(evaluateExpr(argument, recorder.recordArgument()));
        }
        Function function = resolveFunctions(functionCall, arguments, recorder);
        recorder.recordResolvedCallable(function.callableKind(), function.name(), function.arity());
        PeelValue result;
        EvaluationEnvironment currentEnv = environment;
        environment = currentEnv.spawnChild();
        try {
            result = function.runWithTrace(recorder, arguments.toArray(new PeelValue[0]));
        } catch (ReturnValueFlow returnValueFlow) {
            result = returnValueFlow.getValue();
        }
        environment = currentEnv;
        recorder.recordResult(result);
        return result;
    }

    private Function resolveFunctions(Expression.FunctionCall functionCall, List<PeelValue> arguments, FunctionCallRecorder recorder) {
        List<Function> matchingFunctions = getMatchingFunctions(functionCall.callee(), recorder);
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

    private List<Function> getMatchingFunctions(Expression callee, FunctionCallRecorder recorder) {
        return switch (callee) {
            case Expression.VariableName variableName -> {
                recorder.functionFromVar(variableName);
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
                PeelValue value = evaluateExpr(callee, recorder.functionFromExpr());
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

    private PeelValue evaluateOperator(
            Expression.BinaryOperator operator,
            BinaryOpRecorder recorder
    ) {
        recorder.setOperator(operator.operator());
        if (operator.operator().equals("&&")) {
            return evaluateLogicalAnd(operator.lhs(), operator.rhs(), recorder);
        }
        if (operator.operator().equals("||")) {
            return evaluateLogicalOr(operator.lhs(), operator.rhs(), recorder);
        }
        PeelValue lhs = evaluateExpr(operator.lhs(), recorder.recordLhs());
        PeelValue rhs = evaluateExpr(operator.rhs(), recorder.recordRhs());
        List<OperatorDef> candidates = environment.getOperator(operator.operator());
        PeelValue value = operatorResolver.resolveAndApply(operator.operator(), lhs, rhs, candidates);
        recorder.recordValue(value);
        return value;
    }

    private PeelValue evaluateLogicalAnd(
            Expression lhsExpression,
            Expression rhsExpression,
            BinaryOpRecorder recorder
    ) {
        PeelValue lhs = evaluateExpr(lhsExpression, recorder.recordLhs());
        if (!requireBool(lhs)) {
            recorder.setShortCircuit();
            PeelValue bool = PeelValue.bool(false);
            recorder.recordValue(bool);
            return bool;
        }
        PeelValue rhs = evaluateExpr(rhsExpression, recorder.recordRhs());
        PeelValue bool = PeelValue.bool(requireBool(rhs));
        recorder.recordValue(bool);
        return bool;
    }

    private PeelValue evaluateLogicalOr(Expression lhsExpression, Expression rhsExpression, BinaryOpRecorder recorder) {
        PeelValue lhs = evaluateExpr(lhsExpression, recorder.recordLhs());
        if (requireBool(lhs)) {
            recorder.setShortCircuit();
            PeelValue bool = PeelValue.bool(true);
            recorder.recordValue(bool);
            return bool;
        }
        PeelValue rhs = evaluateExpr(rhsExpression, recorder.recordRhs());
        PeelValue out = PeelValue.bool(requireBool(rhs));
        recorder.recordValue(out);
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
}
