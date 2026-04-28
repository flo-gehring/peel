package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.trace.CallableKind;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceExpression.FunctionExecutionTrace;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FunctionCallRecorder implements TraceRecorder {

    private String name;
    private final List<ExpressionRecorder> argumentRecorder = new ArrayList<>();
    private final List<TraceExpression.ParameterBinding> parameterBindings = new ArrayList<>();
    private BlockTraceRecorder blockTraceRecorder;
    private ExpressionRecorder functionSourceRecorder;
    private TraceExpression.CalleeSource calleeSource;
    private TraceExpression.ResolvedCallable resolvedCallable;
    private TraceValue value;

    @Override
    public TraceExpression traceExpression() {
        TraceExpression.CalleeSource effectiveSource = resolveCalleeSource();
        if (effectiveSource == null) {
            throw new IllegalStateException("Function call trace is missing calleeSource");
        }
        if (resolvedCallable == null) {
            throw new IllegalStateException("Function call trace is missing resolvedCallable");
        }
        String displayName = resolveDisplayName(effectiveSource);
        return new TraceExpression.FunctionCall(
                displayName,
                value,
                argumentRecorder.stream().map(ExpressionRecorder::traceExpression).toList(),
                blockTraceRecorder != null ?
                        Optional.of(getFunctionExecutionTrace())
                        : Optional.empty(),
                effectiveSource,
                resolvedCallable
        );
    }

    private String resolveDisplayName(TraceExpression.CalleeSource effectiveSource) {
        if (name != null) {
            return name;
        }
        return switch (effectiveSource) {
            case TraceExpression.CalleeSource.Expression _ -> "<computed_call>";
            case TraceExpression.CalleeSource.VariableName(var varName) -> varName;
        };
    }

    private TraceExpression.CalleeSource resolveCalleeSource() {
        if (calleeSource != null) {
            return calleeSource;
        }
        if (functionSourceRecorder == null) {
            return null;
        }
        return TraceExpression.CalleeSource.expression(functionSourceRecorder.traceExpression());
    }

    private FunctionExecutionTrace getFunctionExecutionTrace() {
        return new FunctionExecutionTrace(
                parameterBindings,
                (TraceExpression.Block) blockTraceRecorder.traceExpression()
        );
    }

    public ExpressionRecorder recordArgument() {
        argumentRecorder.add(new ExpressionRecorder());
        return argumentRecorder.getLast();
    }

    public void recordBinding(String name) {
        parameterBindings.add(
                new TraceExpression.ParameterBinding(
                        name,
                        argumentRecorder.get(parameterBindings.size()).traceExpression())
        );
    }

    public BlockTraceRecorder functionBody() {
        blockTraceRecorder = new BlockTraceRecorder();
        return blockTraceRecorder;
    }

    public void functionFromVar(Expression.VariableName variableName) {
        this.calleeSource = TraceExpression.CalleeSource.variable(variableName.name());
        this.name = variableName.name();
    }

    public ExpressionRecorder functionFromExpr() {
        functionSourceRecorder = new ExpressionRecorder();
        return functionSourceRecorder;
    }

    public void recordResolvedCallable(CallableKind kind, String name, int arity) {
        this.resolvedCallable = new TraceExpression.ResolvedCallable(
                Objects.requireNonNull(kind, "kind"),
                Objects.requireNonNull(name, "name"),
                arity
        );
    }

    public void recordResult(PeelValue result) {
        this.value = TraceValueMapper.fromPeelValue(result);
    }
}