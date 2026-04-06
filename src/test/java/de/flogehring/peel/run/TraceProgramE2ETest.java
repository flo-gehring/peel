package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceProgramE2ETest {

    @Test
    void runtimeReturnsTraceProgramWithResult() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(PeelGrammar.parse("""
                var x = 1;
                x + 2;
                """));

        assertThat(trace.expressions()).hasSize(2);
        assertThat(trace.result()).isEqualTo(new TraceValue.IntegerValue(3));
    }

    @Test
    void logicalOrShortCircuitIsTraced() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(PeelGrammar.parse("True || unknownVar;"));

        assertThat(trace.expressions()).hasSize(1);
        assertThat(trace.result()).isEqualTo(new TraceValue.BoolValue(true));
        assertThat(trace.expressions().getFirst()).isInstanceOf(TraceExpression.LogicalBinaryOperator.class);

        TraceExpression.LogicalBinaryOperator logical = (TraceExpression.LogicalBinaryOperator) trace.expressions().getFirst();
        assertThat(logical.shortCircuited()).isTrue();
        assertThat(logical.rhs()).isEmpty();
    }

    @Test
    void whileLoopContainsTerminatingConditionIteration() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(PeelGrammar.parse("""
                var x = 0;
                while (!(x == 2)) {
                    x = x + 1;
                }
                """));

        assertThat(trace.expressions()).hasSize(2);
        assertThat(trace.expressions().get(1)).isInstanceOf(TraceExpression.WhileLoop.class);

        TraceExpression.WhileLoop loop = (TraceExpression.WhileLoop) trace.expressions().get(1);
        assertThat(loop.iterations()).hasSize(3);
        assertThat(loop.iterations().getLast().body()).isNull();
        assertThat(loop.value()).isEqualTo(new TraceValue.IntegerValue(2));
    }
}
