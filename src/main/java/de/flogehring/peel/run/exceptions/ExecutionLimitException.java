package de.flogehring.peel.run.exceptions;

public final class ExecutionLimitException extends PeelException {
    public enum Limit {TIME_LIMIT, STEP_LIMIT, DEPTH_LIMIT, TRACE_LIMIT, VALUE_LIMIT}

    private final Limit limit;

    public ExecutionLimitException(Limit limit) {
        super("PEEL execution exceeded " + limit);
        this.limit = limit;
    }

    public Limit limit() {
        return limit;
    }
}
