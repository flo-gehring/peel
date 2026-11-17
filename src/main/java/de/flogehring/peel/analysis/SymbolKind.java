package de.flogehring.peel.analysis;

/**
 * Syntactic category of a declared or provided name, without value or type inference.
 * A variable containing a function remains a {@link #VARIABLE}.
 */
public enum SymbolKind {
    /**
     * A variable declaration, loop variable, or provided value.
     */
    VARIABLE,
    /**
     * A parameter of a declared function or lambda.
     */
    PARAMETER,
    /**
     * A named function declaration or a function provided by the host.
     */
    FUNCTION
}
