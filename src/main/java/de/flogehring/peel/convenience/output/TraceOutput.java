package de.flogehring.peel.convenience.output;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceProgram;

import java.util.Map;
import java.util.Objects;

public final class TraceOutput {

    private TraceOutput() {
    }

    public static Map<String, Object> asMap(TraceProgram traceProgram) {
        Objects.requireNonNull(traceProgram, "traceProgram");
        return TraceMapOutput.fromProgram(traceProgram);
    }

    public static Map<String, Object> asMap(TraceExpression traceExpression) {
        Objects.requireNonNull(traceExpression, "traceExpression");
        return TraceMapOutput.fromExpression(traceExpression);
    }

    public static String asJson(TraceProgram traceProgram) {
        Objects.requireNonNull(traceProgram, "traceProgram");
        return TraceJsonOutput.toJson(TraceMapOutput.fromProgram(traceProgram));
    }

    public static String asJson(TraceExpression traceExpression) {
        Objects.requireNonNull(traceExpression, "traceExpression");
        return TraceJsonOutput.toJson(TraceMapOutput.fromExpression(traceExpression));
    }
}
