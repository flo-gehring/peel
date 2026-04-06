package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

public interface TraceRecorder {

    void append(TraceExpression expression);

    void append(TraceRecorder traceRecorder);

    TraceExpression traceExpression();
}
