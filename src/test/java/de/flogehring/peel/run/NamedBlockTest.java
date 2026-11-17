package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.convenience.output.TraceMapOutput;
import de.flogehring.peel.core.eval.RequestBindings;
import de.flogehring.peel.core.lang.BlockCatalog;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NamedBlockTest {
    @Test
    void namesAreOptionalAndPreserveLoopBindingsAndScope() {
        var program = PeelGrammar.parse("""
                calculation: {
                    var sum = 0;
                    for (x in [2, 3]) step: { sum = sum + x; }
                    if (False) absent: { sum = 99; }
                    { sum = sum + 1; }
                    sum;
                }
                """);
        assertThat(BlockCatalog.analyze(program).names()).containsExactly("calculation", "step", "absent");
        var trace = RuntimeFactory.defaultLanguage().run(program, RequestBindings.empty());
        var outer = (TraceExpression.Block) trace.expressions().getFirst();
        assertThat(outer.name()).isEqualTo("calculation");
        var loop = (TraceExpression.ForEachLoop) outer.content().get(1);
        assertThat(loop.iterations()).hasSize(2);
        assertThat(loop.iterations()).allSatisfy(iteration -> assertThat(iteration.body().name()).isEqualTo("step"));
        assertThat(TraceMapOutput.fromExpression(outer)).containsEntry("name", "calculation");
        assertThat(TraceMapOutput.fromValue(trace.result())).containsEntry("value", 6);
    }

    @Test
    void functionBodiesRetainNamesOnEveryCall() {
        var program = PeelGrammar.parse("fun double(x) body: { return x * 2; } double(2); double(3);");
        var trace = RuntimeFactory.defaultLanguage().run(program, RequestBindings.empty());
        assertThat(trace.expressions().stream().filter(TraceExpression.FunctionCall.class::isInstance)
                .map(TraceExpression.FunctionCall.class::cast)).allSatisfy(call ->
                assertThat(call.subEvaluation().orElseThrow().bodyEvaluation().name()).isEqualTo("body"));
    }

    @Test
    void parsesWithoutBlockAnalysisAndChecksDuplicatesOnlyWhenRequested() {
        var program = PeelGrammar.parse("if (False) same: {} same: {}");
        assertThat(program).isNotNull();
        assertThatThrownBy(() -> BlockCatalog.analyze(program))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("same");
    }

    @Test
    void mapLiteralsAndAnonymousBlocksRemainDistinct() {
        assertThatCode(() -> PeelGrammar.parse("var m = {\"x\": 1}; { var x = m[\"x\"]; }"))
                .doesNotThrowAnyException();
    }
}
