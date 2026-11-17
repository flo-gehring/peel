package de.flogehring.peel.analysis;

/**
 * Description of a name supplied to the analyzer without a concrete runtime value.
 * The argument list passed to {@link PeelAnalysis#analyze} determines its origin.
 *
 * @param name        valid PEEL identifier, as checked by {@link PeelAnalysis#isIdentifier(String)}
 * @param kind        category of the provided binding, normally VARIABLE or FUNCTION
 * @param description documentation for completion and hover; null is normalized to an empty string
 * @param arity       function parameter count, or -1 for a non-function or unknown arity
 */
public record ProvidedSymbol(String name, SymbolKind kind, String description, int arity) {
}
