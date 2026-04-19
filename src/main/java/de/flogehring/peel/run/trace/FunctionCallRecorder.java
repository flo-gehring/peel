package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FunctionCallRecorder implements TraceRecorder {

    @Setter
    private String name;
    private final List<ExpressionRecorder> argumentRecorder = new ArrayList<>();
    private final List<TraceExpression.ParameterBinding> parameterBindings = new ArrayList<>();
    private BlockTraceRecorder blockTraceRecorder;
    private ExpressionRecorder functionResolverRecorder;
    private TraceValue value;

    @Override
    public TraceExpression traceExpression() { // TODO
        return new TraceExpression.FunctionCall(
                name,
                value,
                argumentRecorder.stream().map(ExpressionRecorder::traceExpression).toList(),
                Optional.of(new TraceExpression.FunctionCall.FunctionExecutionTrace(
                        parameterBindings,
                        (TraceExpression.Block) blockTraceRecorder.traceExpression()
                ))
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
    }

    public ExpressionRecorder functionFromExpr() {
        functionResolverRecorder = new ExpressionRecorder();
        return functionResolverRecorder;
    }

    public void recordResult(PeelValue result) {
        this.value = TraceValueMapper.fromPeelValue(result);
    }
}
