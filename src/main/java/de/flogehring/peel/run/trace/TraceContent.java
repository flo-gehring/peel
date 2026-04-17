package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

public sealed interface TraceContent {

    TraceExpression expression();

    record Expr(TraceExpression expression) implements TraceContent {

    }

    record Recorder(TraceRecorder traceRecorder) implements TraceContent {
        @Override
        public TraceExpression expression() {
            return traceRecorder.traceExpression();
        }
    }
}
