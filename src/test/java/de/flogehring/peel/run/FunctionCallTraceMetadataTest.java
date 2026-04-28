package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.trace.CallableKind;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionCallTraceMetadataTest {

    @Test
    void peelFunctionCallUsesPeelFunctionKind() {
        TraceProgram trace = run("""
                fun inc(x) {
                    x + 1;
                }
                inc(2);
                """
        );
        TraceExpression.FunctionCall call = functionCallAt(trace, 1);
        assertThat(call.resolvedCallable().kind()).isEqualTo(CallableKind.PEEL_FUNCTION);
        assertThat(call.calleeSource()).isEqualTo(TraceExpression.CalleeSource.variable("inc"));
    }

    @Test
    void closureCallFromVariableUsesClosureKind() {
        TraceProgram trace = run("""
                var inc = fun(x) { x + 1; };
                inc(2);
                """);

        TraceExpression.FunctionCall call = functionCallAt(trace, 1);
        assertThat(call.resolvedCallable().kind()).isEqualTo(CallableKind.CLOSURE);
        assertThat(call.name()).isEqualTo("inc");
        assertThat(call.calleeSource()).isEqualTo(TraceExpression.CalleeSource.variable("inc"));
    }

    @Test
    void hostFunctionCallUsesHostFunctionKind() {
        TraceProgram trace = run("count(\"banana\", \"a\");");
        TraceExpression.FunctionCall call = functionCallAt(trace, 0);
        assertThat(call.resolvedCallable().kind()).isEqualTo(CallableKind.HOST_FUNCTION);
        assertThat(call.calleeSource()).isEqualTo(TraceExpression.CalleeSource.variable("count"));
    }

    @Test
    void hostFunctionAssignedToVariableStillUsesHostFunctionKind() {
        TraceProgram trace = run("""
                var c = count;
                c("banana", "a");
                """);

        TraceExpression.FunctionCall call = functionCallAt(trace, 1);
        assertThat(call.resolvedCallable().kind()).isEqualTo(CallableKind.HOST_FUNCTION);
        assertThat(call.resolvedCallable().name()).isEqualTo("count");
        assertThat(call.calleeSource()).isEqualTo(new TraceExpression.CalleeSource.VariableName("c"));
    }

    @Test
    void closureFromMapSelectorIsTrackedAsExpressionSource() {
        TraceProgram trace = run("""
                var ops = {"inc": fun(x) { x + 1; }};
                ops["inc"](2);
                """);

        TraceExpression.FunctionCall call = functionCallAt(trace, 1);
        assertThat(call.name()).isEqualTo("<computed_call>");
        assertThat(call.resolvedCallable().kind()).isEqualTo(CallableKind.CLOSURE);
        assertThat(call.calleeSource()).isInstanceOf(TraceExpression.CalleeSource.Expression.class);
        TraceExpression.CalleeSource.Expression source = (TraceExpression.CalleeSource.Expression) call.calleeSource();
        assertThat(source.expression()).isInstanceOf(TraceExpression.Selector.class);
    }

    @Test
    void immediateClosureInvocationIsTrackedAsExpressionSource() {
        TraceProgram trace = run("(fun(x) { x + 1; })(2);");

        TraceExpression.FunctionCall call = functionCallAt(trace, 0);
        assertThat(call.name()).isEqualTo("<computed_call>");
        assertThat(call.resolvedCallable().kind()).isEqualTo(CallableKind.CLOSURE);
        assertThat(call.calleeSource()).isInstanceOf(TraceExpression.CalleeSource.Expression.class);
        TraceExpression.CalleeSource.Expression source = (TraceExpression.CalleeSource.Expression) call.calleeSource();
        assertThat(source.expression()).isInstanceOf(TraceExpression.Literal.class);
    }

    private static TraceProgram run(String source) {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        return runtime.run(PeelGrammar.parse(source));
    }

    private static TraceExpression.FunctionCall functionCallAt(TraceProgram trace, int index) {
        assertThat(trace.expressions().get(index)).isInstanceOf(TraceExpression.FunctionCall.class);
        return (TraceExpression.FunctionCall) trace.expressions().get(index);
    }
}
