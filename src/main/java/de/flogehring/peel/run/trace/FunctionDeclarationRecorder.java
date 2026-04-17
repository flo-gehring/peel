package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;

public class FunctionDeclarationRecorder implements TraceRecorder {
    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.Literal(TraceValue.none()); // TODO
    }
}
