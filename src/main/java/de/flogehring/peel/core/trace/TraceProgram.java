package de.flogehring.peel.core.trace;

import java.util.List;
import java.util.Map;

public record TraceProgram(List<TraceExpression> expressions, TraceValue result, Map<String, TraceValue> variablesAtExit) {
    public TraceProgram {
        variablesAtExit = Map.copyOf(variablesAtExit);
    }

    public TraceProgram(List<TraceExpression> expressions, TraceValue result) {
        this(expressions, result, Map.of());
    }
}
