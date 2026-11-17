package de.flogehring.peel.analysis;

/**
 * Machine-readable classification of a finding, independent of its explanatory message.
 */
public enum DiagnosticCode {
    /**
     * The lexer or parser could not recognize part of the source.
     */
    SYNTAX_ERROR,
    /**
     * A read or call could not be resolved to a declared or provided name.
     */
    UNKNOWN_NAME,
    /**
     * Assignment targets an undeclared name or a provided, read-only name.
     */
    UNDECLARED_ASSIGNMENT,
    /**
     * A name is redeclared in the same scope, except for function overloads.
     */
    DUPLICATE_DECLARATION,
    /**
     * An occurrence resolves to a variable that has not been initialized.
     */
    UNINITIALIZED_VARIABLE,
    /**
     * A return statement appears outside a function or lambda.
     */
    RETURN_OUTSIDE_FUNCTION,
    /**
     * A block label is used more than once anywhere in the script.
     */
    DUPLICATE_BLOCK_NAME
}
