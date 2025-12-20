package de.flogehring.peel.parse;

public class RedeclaredVariableException extends PeelParsingException {
    public RedeclaredVariableException(String message) {
        super(message);
    }
}
