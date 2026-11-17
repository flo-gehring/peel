package de.flogehring.peel.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NamedBlockAnalysisTest {
    @Test void namedAndAnonymousBlocksPreserveScopeAndDoNotDeclareVariablesForLabels() {
        var source = "var value=1; report:{var value=input; for(item in [1,2]) step:{value+item;} value;} value;";
        var result = PeelAnalysis.analyze(source, List.of(new ProvidedSymbol("input", SymbolKind.VARIABLE, "", -1)), List.of());
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.symbols()).noneMatch(symbol -> List.of("report", "step").contains(symbol.name()));
        var values = result.symbols().stream().filter(symbol -> symbol.name().equals("value")).toList();
        assertThat(values).hasSize(2);
        assertThat(result.occurrences().getLast().symbolId()).isEqualTo(values.getFirst().id());
        assertThat(PeelAnalysis.analyze("{var hidden=1;} hidden;", List.of(), List.of()).diagnostics())
                .extracting(AnalysisResult.Diagnostic::code).containsExactly(DiagnosticCode.UNKNOWN_NAME);
    }

    @Test void detectsDuplicateBlockNamesAcrossBranchesAndFunctionsButAllowsSameVariableName() {
        var result = PeelAnalysis.analyze("if(False) same:{} fun f() body:{same:{}}", List.of(), List.of());
        assertThat(result.diagnostics()).extracting(AnalysisResult.Diagnostic::code).containsExactly(DiagnosticCode.DUPLICATE_BLOCK_NAME);
        assertThat(PeelAnalysis.analyze("var same=1; same:{same;}", List.of(), List.of()).diagnostics()).isEmpty();
    }

    @Test void handlesNamedFunctionBodiesAndSingleStatementLoopBodies() {
        var result = PeelAnalysis.analyze("fun f(x) body:{return x;} for(item in [1,2]) f(item);", List.of(), List.of());
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test void survivesTypingNamedBlocksAndKeepsMissingOuterBraceOpenAfterAnInnerBrace() {
        var source = "outer:{var value=1; inner:{}";
        for (int i=0; i<=source.length(); i++) {
            var prefix=source.substring(0,i);
            assertThatCode(() -> PeelAnalysis.analyze(prefix, List.of(), List.of())).doesNotThrowAnyException();
        }
        var result=PeelAnalysis.analyze(source, List.of(), List.of());
        var variable=result.symbols().stream().filter(symbol -> symbol.name().equals("value")).findFirst().orElseThrow();
        var scope=result.scopes().stream().filter(item -> item.id()==variable.scopeId()).findFirst().orElseThrow();
        assertThat(scope.openEnded()).isTrue();
        assertThat(scope.end()).isEqualTo(source.length());
    }
}
