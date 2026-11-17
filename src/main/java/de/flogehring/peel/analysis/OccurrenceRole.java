package de.flogehring.peel.analysis;

/**
 * Use of a resolved symbol at one source location, independent of its kind.
 */
public enum OccurrenceRole {
    /**
     * Introduces a name, including an additional declaration of a function overload.
     */
    DECLARATION,
    /**
     * Reads a name as a value.
     */
    REFERENCE,
    /**
     * Uses a name as an assignment target.
     */
    ASSIGNMENT,
    /**
     * Uses a name directly as the callee of a function call.
     */
    CALL
}
