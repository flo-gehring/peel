package de.flogehring.peel.core.trace;

import java.util.List;

public record TraceProgram(
        List<TraceExpression> expressions,
        TraceValue result
) {

    public static TraceProgram fromExpressions(List<TraceExpression> expressions) {
        if (expressions.isEmpty()) {
            return new TraceProgram(expressions, TraceValue.none());
        }
        return new TraceProgram(expressions, expressions.getLast().value());
    }
}
