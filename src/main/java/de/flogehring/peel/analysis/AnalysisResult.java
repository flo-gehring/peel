package de.flogehring.peel.analysis;

import java.util.List;

/**
 * Editor-independent snapshot of the names, uses, scopes and findings in one source text.
 * Analysis does not evaluate the source. Symbols and occurrences are deliberately flat:
 * {@link Occurrence#symbolId()} refers to {@link Symbol#id()} in this result.
 * Symbol and scope IDs are local to this snapshot and must not be persisted across analyses.
 * All source ranges use zero-based UTF-16 offsets with an exclusive end.
 *
 * @param symbols     declared and provided names, including names without source occurrences
 * @param occurrences resolved declarations and uses; unknown names produce diagnostics instead
 * @param scopes      lexical scope tree, linked by parent IDs
 * @param diagnostics syntax errors and statically detected name or scope problems
 */
public record AnalysisResult(
        List<Symbol> symbols,
        List<Occurrence> occurrences,
        List<LexicalScope> scopes,
        List<Diagnostic> diagnostics
) {

    /**
     * One name binding, distinct from same-named bindings in other scopes.
     * Function overloads in the same scope share a symbol; its arity describes the first one.
     * Provided symbols have no source declaration: their start and end are both zero.
     *
     * @param id          unique symbol ID within this result, referenced by occurrences
     * @param name        PEEL identifier
     * @param kind        declaration category; does not infer the type of a variable's value
     * @param origin      whether the binding comes from the source, consumer, or runtime
     * @param description consumer-supplied documentation, or an empty string
     * @param arity       function parameter count, or -1 for non-functions or unknown arity
     * @param scopeId     declaring scope ID; provided symbols belong to outer scope zero
     * @param visibleFrom first UTF-16 offset eligible for completion, subject to scope visibility
     * @param start       inclusive offset of the first declaration's identifier, or zero if provided
     * @param end         exclusive offset of that identifier, or zero if provided
     */
    public record Symbol(
            int id,
            String name,
            SymbolKind kind,
            SymbolOrigin origin,
            String description,
            int arity,
            int scopeId,
            int visibleFrom,
            int start,
            int end
    ) {
    }

    /**
     * One resolved occurrence of a symbol in the source.
     * Its role describes usage, so a CALL may refer to a VARIABLE containing a function.
     *
     * @param symbolId ID of the referenced symbol in the same result
     * @param role     declaration, read, assignment target, or direct call
     * @param start    inclusive UTF-16 offset of the identifier
     * @param end      exclusive UTF-16 offset of the identifier
     */
    public record Occurrence(int symbolId, OccurrenceRole role, int start, int end) {
    }

    /**
     * Lexical visibility region. Names resolve in this scope before its ancestors.
     * Scope zero holds provided names; the program and nested bodies create child scopes.
     * Combine scope membership with {@link Symbol#visibleFrom()} to determine visible names.
     *
     * @param id        unique scope ID within this result
     * @param parentId  enclosing scope ID, or -1 for outer scope zero
     * @param start     inclusive UTF-16 offset of the scope
     * @param end       exclusive UTF-16 offset of the scope
     * @param openEnded whether a completion cursor at end still belongs to this scope;
     *                  true for the program, provided scope, and unfinished bodies
     */
    public record LexicalScope(int id, int parentId, int start, int end, boolean openEnded) {
    }

    /**
     * Static finding attached to a source range; not an execution result or complete type check.
     *
     * @param code     machine-readable finding category
     * @param severity warning or error classification
     * @param message  human-readable explanation; consumers should branch on code instead
     * @param start    inclusive UTF-16 offset of the affected source
     * @param end      exclusive UTF-16 offset; may equal start for an end-of-input error
     */
    public record Diagnostic(DiagnosticCode code, DiagnosticSeverity severity, String message, int start, int end) {
    }
}
