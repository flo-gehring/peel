package de.flogehring.peel.run.exceptions;

public class NumericOperatorOverrideException extends PeelException {

    public NumericOperatorOverrideException(String symbol) {
        super("Registering typed numeric operator ''{0}'' is blocked by default. Enable advanced numeric overrides explicitly to allow this.", symbol);
    }
}
