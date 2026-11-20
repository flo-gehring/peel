package de.flogehring.peel.run.exceptions;

public final class PeelInterruptedException extends PeelException {
    public PeelInterruptedException() {
        super("PEEL execution interrupted");
    }
}
