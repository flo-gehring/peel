package de.flogehring.peel.core.eval;

import java.time.Duration;

/**
 * Per-evaluation limits. Value size measures text characters, decimal digits or collection entries.
 */
public record ExecutionLimits(Duration timeout, long maxSteps, int maxDepth, long maxTraceUnits, int maxValueSize) {
    public ExecutionLimits {
        if (timeout == null || timeout.isNegative() || timeout.isZero() || maxSteps < 1
                || maxDepth < 1 || maxTraceUnits < 1 || maxValueSize < 1) {
            throw new IllegalArgumentException("Execution limits must be positive");
        }
        try {
            timeout.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Execution timeout is too large to represent in nanoseconds", exception);
        }
    }
}
