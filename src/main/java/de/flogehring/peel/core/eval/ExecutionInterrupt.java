package de.flogehring.peel.core.eval;

import de.flogehring.peel.run.exceptions.PeelInterruptedException;

public final class ExecutionInterrupt {
    private ExecutionInterrupt() {
    }

    /**
     * Checks for cancellation without clearing the caller's interrupt flag.
     */
    public static void check() {
        if (Thread.currentThread().isInterrupted()) {
            throw new PeelInterruptedException();
        }
    }
}
