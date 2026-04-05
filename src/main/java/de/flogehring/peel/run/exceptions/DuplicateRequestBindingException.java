package de.flogehring.peel.run.exceptions;

public class DuplicateRequestBindingException extends PeelException {

    public DuplicateRequestBindingException(String name) {
        super("Request binding ''{0}'' conflicts with an existing global runtime variable", name);
    }
}
