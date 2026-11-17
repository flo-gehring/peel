package de.flogehring.peel.analysis;

import java.util.List;

/** All ranges use zero-based UTF-16 offsets with an exclusive end. */
public record AnalysisResult(List<Symbol> symbols, List<Occurrence> occurrences,
                             List<LexicalScope> scopes, List<Diagnostic> diagnostics) {
    public record Symbol(int id, String name, String kind, String origin, String description,
                         int arity, int scopeId, int visibleFrom, int start, int end) {}
    public record Occurrence(int symbolId, String role, int start, int end) {}
    public record LexicalScope(int id, int parentId, int start, int end, boolean openEnded) {}
    public record Diagnostic(String code, String severity, String message, int start, int end) {}
}
