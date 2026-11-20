package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.ExecutionLimits;
import de.flogehring.peel.core.eval.RequestBindings;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peel.run.SimpleFunction;
import de.flogehring.peel.run.exceptions.ExecutionLimitException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static de.flogehring.peel.run.exceptions.ExecutionLimitException.Limit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionLimitsTest {
    @Test
    void rejectsAnOverflowingTimeoutAtConstruction() {
        assertThatThrownBy(() -> new ExecutionLimits(Duration.ofSeconds(Long.MAX_VALUE), 100, 64, 100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Execution timeout is too large to represent in nanoseconds")
                .hasCauseInstanceOf(ArithmeticException.class);
    }

    @Test
    void limitsEvaluatorAndBuiltinWork() {
        var limits = limits(100, 64, 100_000, 4_096);
        expectLimit("while (1 == 1) {}", limits, STEP_LIMIT);
        expectLimit("count(\"text\", \"\");", limits, STEP_LIMIT);
    }

    @Test
    void limitsRecursiveCalls() {
        expectLimit("fun f(n) { f(n + 1); } f(0);", limits(10_000, 16, 100_000, 4_096), DEPTH_LIMIT);
    }

    @Test
    void chargesRepeatedSnapshotsOfTheSameValue() {
        String script = "var s=\"%s\"; var n=0; while (!(n == 60)) { n=n+1; }";
        var limits = limits(10_000, 64, 6_000, 4_096);
        assertThat(run(script.formatted("a"), limits)).isNotNull();
        expectLimit(script.formatted("a".repeat(256)), limits, TRACE_LIMIT);
    }

    @Test
    void checksGrowthBeforeConstructingOversizedValues() {
        var limits = limits(10_000, 64, 100_000, 16);
        expectLimit("\"abcdefghij\" + \"abcdefghij\";", limits, VALUE_LIMIT);
        expectLimit("1.23 ** 1000000;", limits, VALUE_LIMIT);
    }

    @Test
    void expiredEvaluationLeavesTheThreadAvailableForAnotherRun() {
        var runtime = RuntimeBuilder.standardLanguage().build();
        var loop = PeelGrammar.parse("while (1 == 1) {}");
        var limits = new ExecutionLimits(Duration.ofNanos(1), 10_000, 64, 100_000, 4_096);
        assertThatThrownBy(() -> runtime.run(loop, RequestBindings.empty(), limits))
                .isInstanceOfSatisfying(ExecutionLimitException.class, error -> assertThat(error.limit()).isEqualTo(TIME_LIMIT));
        assertThat(runtime.run(PeelGrammar.parse("2 + 3;"), RequestBindings.empty(),
                limits(100, 64, 100, 4_096)).result()).isEqualTo(new TraceValue.IntegerValue(5));
    }

    @Test
    void nestedRuntimeCallsUseIndependentBudgets() {
        var inner = RuntimeBuilder.standardLanguage().build();
        var innerProgram = PeelGrammar.parse("1 ** 50;");
        var outer = RuntimeBuilder.standardLanguage()
                .withFunction(new SimpleFunction("nested", 0, _ -> {
                    assertThatThrownBy(() -> inner.run(innerProgram, RequestBindings.empty(),
                            limits(10, 64, 100, 4_096)))
                            .isInstanceOfSatisfying(ExecutionLimitException.class,
                                    error -> assertThat(error.limit()).isEqualTo(STEP_LIMIT));
                    var innerResult = inner.run(innerProgram, RequestBindings.empty(),
                            limits(100, 64, 100, 4_096));
                    assertThat(innerResult.result()).isEqualTo(new TraceValue.IntegerValue(1));
                    return PeelValue.integer(5);
                }))
                .build();

        var result = outer.run(PeelGrammar.parse("nested();"), RequestBindings.empty(),
                limits(10, 64, 100, 4_096));

        assertThat(result.result()).isEqualTo(new TraceValue.IntegerValue(5));
    }

    private static void expectLimit(String script, ExecutionLimits limits, ExecutionLimitException.Limit limit) {
        assertThatThrownBy(() -> run(script, limits)).isInstanceOfSatisfying(
                ExecutionLimitException.class, error -> assertThat(error.limit()).isEqualTo(limit));
    }

    private static TraceProgram run(String script, ExecutionLimits limits) {
        return RuntimeBuilder.standardLanguage().build().run(PeelGrammar.parse(script), RequestBindings.empty(), limits);
    }

    private static ExecutionLimits limits(long steps, int depth, long trace, int valueSize) {
        return new ExecutionLimits(Duration.ofSeconds(5), steps, depth, trace, valueSize);
    }
}
