package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

public final class SingleTraceExpressionRecorder implements TraceRecorder {


    TraceContent traceContent;

    public SingleTraceExpressionRecorder() {
    }


    public TraceExpression expression() {
        return traceContent.expression();
    }

    @Override
    public TraceExpression traceExpression() {
        return traceContent.expression();
    }
}
