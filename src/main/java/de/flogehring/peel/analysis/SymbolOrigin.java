package de.flogehring.peel.analysis;

/**
 * Source of a symbol's declaration; independent of its {@link SymbolKind}.
 */
public enum SymbolOrigin {
    /**
     * Declared in the analyzed source.
     */
    LOCAL,
    /**
     * Supplied by the consumer, for example an expected script input.
     */
    EXTERNAL,
    /**
     * Supplied by the configured runtime library.
     */
    RUNTIME
}
