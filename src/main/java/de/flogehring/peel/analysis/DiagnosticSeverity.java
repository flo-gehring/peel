package de.flogehring.peel.analysis;

/**
 * Severity of a static analysis finding; analysis does not execute the script.
 */
public enum DiagnosticSeverity {
    /**
     * A potentially missing name, such as an input not supplied to the analyzer.
     */
    WARNING,
    /**
     * A syntax error or a detected violation of the language's name and scope rules.
     */
    ERROR
}
