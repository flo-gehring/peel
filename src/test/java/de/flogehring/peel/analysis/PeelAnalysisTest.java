package de.flogehring.peel.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PeelAnalysisTest {

    @Test
    void survivesEveryTypingPrefixAndKeepsRuntimeCallbacksUnevaluated() {
        var source = "var a = 1; fun f(x) { for (item in [1,2]) { if (True) { return x + item; } } } f(a);";
        for (int i = 0; i <= source.length(); i++) {
            var prefix = source.substring(0, i);
            assertThatCode(() -> PeelAnalysis.analyze(prefix, List.of(), List.of())).doesNotThrowAnyException();
        }
        var symbols = de.flogehring.peel.convenience.RuntimeBuilder.standardLanguage().withFunction(
                de.flogehring.peel.convenience.FunctionFactory.binary("injected", (_, _) -> {
                    throw new AssertionError("Analysis must not execute functions");
                })).symbols();
        var result = PeelAnalysis.analyze("injected(1, 2);", List.of(), symbols);
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.symbols()).anyMatch(s -> s.name().equals("count") && s.arity() == 2);
        assertThat(PeelAnalysis.analyze("var x; x;", List.of(), List.of()).diagnostics())
                .anyMatch(d -> d.code().equals(DiagnosticCode.UNINITIALIZED_VARIABLE));
    }

    @Test
    void resolvesExternalParametersClosuresAndShadowing() {
        var source = "var x = input; fun f(x) { return fun(y) { return x + y; }; } x;";
        var result = PeelAnalysis.analyze(source,
                List.of(new ProvidedSymbol("input", SymbolKind.VARIABLE, "External input", -1)), List.of());
        assertThat(result.diagnostics()).isEmpty();
        var xSymbols = result.symbols().stream().filter(symbol -> symbol.name().equals("x")).toList();
        assertThat(xSymbols).hasSize(2);
        assertThat(result.occurrences().getLast().symbolId()).isEqualTo(xSymbols.getFirst().id());
        assertThat(result.occurrences().stream().filter(o -> o.start() == source.indexOf("x +")).findFirst().orElseThrow().symbolId())
                .isEqualTo(xSymbols.getLast().id());
    }

    @Test
    void distinguishesFunctionReferencesAndCallableVariablesWithoutInference() {
        var result = PeelAnalysis.analyze("fun f(x) { return x; } var g = f; g(1);", List.of(), List.of());
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.symbols().stream().filter(s -> s.name().equals("g")).findFirst().orElseThrow().kind())
                .isEqualTo(SymbolKind.VARIABLE);
        assertThat(result.occurrences().stream().filter(o -> o.role() == OccurrenceRole.CALL).count()).isEqualTo(1);
    }

    @Test
    void reportsUnknownReadsAndInvalidAssignmentsWithoutExecutingAnything() {
        var result = PeelAnalysis.analyze("missing; other = 1; return 2; var x = x;", List.of(), List.of());
        assertThat(result.diagnostics()).extracting(AnalysisResult.Diagnostic::code)
                .contains(DiagnosticCode.UNKNOWN_NAME, DiagnosticCode.UNDECLARED_ASSIGNMENT, DiagnosticCode.RETURN_OUTSIDE_FUNCTION, DiagnosticCode.UNINITIALIZED_VARIABLE);
    }

    @Test
    void handlesEmptyAndIncompleteProgramsAndKeepsCompletedStatements() {
        assertThat(PeelAnalysis.analyze("", List.of(), List.of()).diagnostics()).isEmpty();
        for (var source : List.of("var x = 1; var ", "fun f(x) { return x +", "var x = \"unterminated", "for (x in [1]) { x;"))
            assertThatCode(() -> PeelAnalysis.analyze(source, List.of(), List.of())).doesNotThrowAnyException();
        assertThat(PeelAnalysis.analyze("var x = 1; var ", List.of(), List.of()).symbols()).anyMatch(s -> s.name().equals("x"));
    }

    @Test
    void reportsUtf16OffsetsWithCrLfAndAstralCharacters() {
        var source = "var text = \"🍌\";\r\nunknown;";
        var diagnostic = PeelAnalysis.analyze(source, List.of(), List.of()).diagnostics().getFirst();
        assertThat(diagnostic.start()).isEqualTo(source.indexOf("unknown"));
        assertThat(diagnostic.end()).isEqualTo(source.indexOf("unknown") + 7);
    }

    @Test
    void foreachNamesAreNotVisibleInTheirIterableOrAfterTheLoop() {
        var result = PeelAnalysis.analyze("for (x in x) { x; } x;", List.of(), List.of());
        assertThat(result.diagnostics().stream().filter(d -> d.code().equals(DiagnosticCode.UNKNOWN_NAME)).count()).isEqualTo(2);
    }

    @Test
    void identifierValidationUsesTheLexer() {
        assertThat(PeelAnalysis.isIdentifier("_input2")).isTrue();
        for (var name : List.of("var", "True", "x-y", "a b", "// comment", " x", "x "))
            assertThat(PeelAnalysis.isIdentifier(name)).isFalse();
    }
}
