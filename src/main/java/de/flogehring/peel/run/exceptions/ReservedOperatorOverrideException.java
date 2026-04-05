package de.flogehring.peel.run.exceptions;

public class ReservedOperatorOverrideException extends PeelException {

    public ReservedOperatorOverrideException(String symbol) {
        super(
                "Operator ''{0}'' is reserved for language-level short-circuit evaluation and cannot be overridden.",
                symbol
        );
    }
}
