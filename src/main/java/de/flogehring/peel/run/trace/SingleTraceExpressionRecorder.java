package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

public final class SingleTraceExpressionRecorder implements TraceRecorder {

    private TraceExpression expression;
    private TraceRecorder traceRecorder;

    public SingleTraceExpressionRecorder() {
        this.expression = null;
        this.traceRecorder = null;
    }

    @Override
    public void append(TraceExpression expression) {
        if (this.expression != null) {
            throw new IllegalStateException("Expected only one trace expression");
        }
        this.expression = expression;
    }

    @Override
    public void append(TraceRecorder traceRecorder) {
        if (this.traceRecorder != null) {
            throw new IllegalStateException("Expected only one trace expression");
        }
        this.traceRecorder = traceRecorder;
    }

    public TraceExpression expression() {
        if (expression == null && traceRecorder == null) {
            throw new IllegalStateException("No trace expression captured");
        }
        if (traceRecorder != null) {
            return traceRecorder.traceExpression();
        } else {
            return expression;
        }
    }

    @Override
    public TraceExpression traceExpression() {
        return expression;
    }
}
