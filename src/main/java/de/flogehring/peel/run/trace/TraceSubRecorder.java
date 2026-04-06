package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

import java.util.ArrayList;
import java.util.List;

public final class TraceSubRecorder implements TraceRecorder {

    private final List<TraceRecorder> expressions = new ArrayList<>();

    @Override
    public void append(TraceExpression expression) {
        SingleTraceExpressionRecorder singleTraceExpressionRecorder = new SingleTraceExpressionRecorder();
        singleTraceExpressionRecorder.append(expression);
        expressions.add(singleTraceExpressionRecorder);
    }

    public List<TraceExpression> expressions() {
        return expressions.stream().map(TraceRecorder::traceExpression).toList();
    }

    @Override
    public void append(TraceRecorder traceRecorder) {
        expressions.add(traceRecorder);
    }

    public TraceExpression.Block toBlock() {
        return new TraceExpression.Block(expressions());
    }

    @Override
    public TraceExpression traceExpression() {
        return toBlock();
    }
}
