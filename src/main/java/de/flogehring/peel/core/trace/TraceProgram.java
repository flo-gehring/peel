package de.flogehring.peel.core.trace;

import java.util.List;

public record TraceProgram(
        List<TraceExpression> expressions,
        TraceValue result
) {

}
