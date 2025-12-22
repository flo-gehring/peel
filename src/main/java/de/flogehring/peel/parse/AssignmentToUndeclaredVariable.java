package de.flogehring.peel.parse;

public class AssignmentToUndeclaredVariable extends PeelParsingException {
    public AssignmentToUndeclaredVariable(String message) {
        super(message);
    }
}
