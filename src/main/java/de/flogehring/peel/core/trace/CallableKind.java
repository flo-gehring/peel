package de.flogehring.peel.core.trace;

public enum CallableKind {
    FUNCTION_REFERENCE("function_reference"),
    CLOSURE("closure"),
    PEEL_FUNCTION("peel_function"),
    HOST_FUNCTION("host_function");

    private final String wireValue;

    CallableKind(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
