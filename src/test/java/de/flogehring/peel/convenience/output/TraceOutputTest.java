package de.flogehring.peel.convenience.output;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValue;
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
}
