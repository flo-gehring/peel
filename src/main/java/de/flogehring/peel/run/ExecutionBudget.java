package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.ExecutionInterrupt;
import de.flogehring.peel.core.eval.ExecutionLimits;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.core.values.Text;
import de.flogehring.peel.run.exceptions.ExecutionLimitException;

import java.util.Objects;

import static de.flogehring.peel.run.exceptions.ExecutionLimitException.Limit.*;

public final class ExecutionBudget {
    private static final ExecutionBudget UNLIMITED = new ExecutionBudget();

    public static ExecutionBudget unlimited() {
        return UNLIMITED;
    }

    private final ExecutionLimits limits;
    private final long started;
    private final long timeoutNanos;
    private long stepsLeft;
    private long traceLeft;
    private int depth;

    private ExecutionBudget() {
        limits = null;
        started = 0;
        timeoutNanos = 0;
    }

    ExecutionBudget(ExecutionLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        started = System.nanoTime();
        timeoutNanos = limits.timeout().toNanos();
        stepsLeft = limits.maxSteps();
        traceLeft = limits.maxTraceUnits();
    }

    public void checkpoint() {
        ExecutionInterrupt.check();
        if (limits != null && System.nanoTime() - started >= timeoutNanos) {
            throw new ExecutionLimitException(TIME_LIMIT);
        }
    }

    /**
     * Charge for one unit of internal work in a built-in or extension.
     */
    public void step() {
        checkpoint();
        if (limits != null) {
            if (stepsLeft == 0) throw new ExecutionLimitException(STEP_LIMIT);
            stepsLeft--;
        }
    }

    public void enter() {
        if (limits != null) {
            if (depth == limits.maxDepth()) throw new ExecutionLimitException(DEPTH_LIMIT);
            depth++;
        }
    }

    public void leave() {
        if (limits != null) depth--;
    }

    public void trace(long units) {
        if (units < 0) throw new IllegalArgumentException("Trace units must be nonnegative");
        if (limits != null) {
            checkpoint();
            if (units > traceLeft) throw new ExecutionLimitException(TRACE_LIMIT);
            traceLeft -= units;
        }
    }

    public void requireValueSize(long size) {
        if (limits != null && size > limits.maxValueSize()) {
            throw new ExecutionLimitException(VALUE_LIMIT);
        }
    }

    public static long valueSize(PeelValue value) {
        return switch (value) {
            case Text text -> text.value().length();
            case Number.Decimal decimal -> decimalSize(decimal.value().precision(), decimal.value().scale());
            case PeelValue.Collection.List list -> list.list().size();
            case PeelValue.Collection.Map map -> map.map().size();
            case null, default -> 1;
        };
    }

    public static long decimalSize(long precision, long scale) {
        return Math.max(precision, scale) + Math.max(0, -scale);
    }

    public void checkBinding(PeelValue value) {
        if (limits == null) return;
        step();
        requireValueSize(valueSize(value));
        enter();
        try {
            switch (value) {
                case PeelValue.Collection.List list -> list.list().forEach(this::checkBinding);
                case PeelValue.Collection.Map map -> map.map().forEach((key, item) -> {
                    checkBinding(key);
                    checkBinding(item);
                });
                default -> {
                }
            }
        } finally {
            leave();
        }
    }
}
