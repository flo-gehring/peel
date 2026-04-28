package de.flogehring.peel.convenience.output;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TraceOutputTest {

    @Test
    void mapOutputUsesEntriesForTraceMapValues() {
        TraceExpression.MapLiteral traceMapLiteral = new TraceExpression.MapLiteral(List.of(
                new TraceExpression.MapLiteral.MapEntry(
                        new TraceExpression.Literal(new TraceValue.TextValue("k")),
                        new TraceExpression.Literal(new TraceValue.IntegerValue(1))
                )
        ));
        TraceProgram traceProgram = new TraceProgram(List.of(traceMapLiteral), traceMapLiteral.value());

        Map<String, Object> output = TraceOutput.asMap(traceProgram);

        assertThat(output.get("type")).isEqualTo("program");
        assertThat(output.get("expressions")).isInstanceOf(List.class);

        Map<?, ?> result = (Map<?, ?>) output.get("result");
        assertThat(result.get("type")).isEqualTo("map");

        List<?> entries = (List<?>) result.get("entries");
        assertThat(entries).hasSize(1);
        Map<?, ?> firstEntry = (Map<?, ?>) entries.getFirst();

        Map<?, ?> key = (Map<?, ?>) firstEntry.get("key");
        Map<?, ?> value = (Map<?, ?>) firstEntry.get("value");
        assertThat(key.get("type")).isEqualTo("text");
        assertThat(key.get("value")).isEqualTo("k");
        assertThat(value.get("type")).isEqualTo("integer");
        assertThat(value.get("value")).isEqualTo(1);
    }

    @Test
    void jsonOutputEscapesStringsAndContainsProgramType() {
        TraceExpression expression = new TraceExpression.Literal(new TraceValue.TextValue("a\"b\nc"));
        TraceProgram traceProgram = new TraceProgram(List.of(expression), expression.value());

        String json = TraceOutput.asJson(traceProgram);

        assertThat(json).contains("\"type\":\"program\"");
        assertThat(json).contains("a\\\"b\\nc");
    }

    @Test
    void mapOutputForWhileLoopIncrementToTenHasFinalValueTen() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram traceProgram = runtime.run(PeelGrammar.parse("""
                var i = 0;
                while (!(i == 10)) {
                    i = i + 1;
                }
                """));

        Map<String, Object> output = TraceOutput.asMap(traceProgram);

        Map<?, ?> result = (Map<?, ?>) output.get("result");
        assertThat(result.get("type")).isEqualTo("integer");
        assertThat(result.get("value")).isEqualTo(10);

        List<?> expressions = (List<?>) output.get("expressions");
        Map<?, ?> whileLoop = (Map<?, ?>) expressions.getLast();
        assertThat(whileLoop.get("type")).isEqualTo("while_loop");

        Map<?, ?> whileValue = (Map<?, ?>) whileLoop.get("value");
        assertThat(whileValue.get("type")).isEqualTo("integer");
        assertThat(whileValue.get("value")).isEqualTo(10);
    }

    @Test
    void forEachMapOutputContainsLoopVariableBinding() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram traceProgram = runtime.run(PeelGrammar.parse("""
                for (x in [1, 2, 3]) {
                    x * 2;
                }
                """));

        Map<String, Object> output = TraceOutput.asMap(traceProgram);
        List<?> expressions = (List<?>) output.get("expressions");
        Map<?, ?> forEach = (Map<?, ?>) expressions.getFirst();

        assertThat(forEach.get("type")).isEqualTo("for_each_loop");
        assertThat(forEach.get("variableName")).isEqualTo("x");

        Map<?, ?> iterableExpression = (Map<?, ?>) forEach.get("iterableExpression");
        assertThat(iterableExpression.get("type")).isEqualTo("list_literal");

        List<?> iterations = (List<?>) forEach.get("iterations");
        assertThat(iterations).hasSize(3);
        Map<?, ?> firstIteration = (Map<?, ?>) iterations.getFirst();
        Map<?, ?> binding = (Map<?, ?>) firstIteration.get("binding");
        assertThat(binding.get("name")).isEqualTo("x");

        Map<?, ?> bindingValue = (Map<?, ?>) binding.get("value");
        assertThat(bindingValue.get("type")).isEqualTo("integer");
        assertThat(bindingValue.get("value")).isEqualTo(1);
    }
}
