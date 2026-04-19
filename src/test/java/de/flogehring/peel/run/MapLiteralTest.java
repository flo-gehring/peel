package de.flogehring.peel.run;

import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.values.PeelValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;

class MapLiteralTest {

    @Test
    void mapLiteralWithNumberKeys() {
        runProgrammAndExpect(
                """
                        var m = {1: 10, 2: 20};
                        m[2];
                        """,
                PeelValue.integer(20)
        );
    }

    @Test
    void mapLiteralWithBoolKeys() {
        runProgrammAndExpect(
                """
                        var m = {True: 1, False: 2};
                        m[False];
                        """,
                PeelValue.integer(2)
        );
    }

    @Test
    void mapLiteralWithTextKeys() {
        runProgrammAndExpect(
                """
                        var m = {"a": 1, "b": 2};
                        m["b"];
                        """,
                PeelValue.integer(2)
        );
    }

    @Test
    void mapLiteralWithMixedKeyTypes() {
        runProgrammAndExpect(
                """
                        var m = {1: 10, True: 20, "x": 30};
                        m;
                        """,
                new TraceValue.MapValue(List.of(
                        new TraceValue.MapValue.MapEntry(TraceValue.integer(1), TraceValue.integer(10)),
                        new TraceValue.MapValue.MapEntry(TraceValue.bool(true), TraceValue.integer(20)),
                        new TraceValue.MapValue.MapEntry(TraceValue.text("x"), TraceValue.integer(30))

                ))
        );
    }

    @Test
    void mapLiteralMixedKeySelectors() {
        runProgrammAndExpect(
                """
                        var m = {1: 10, True: 20, "x": 30};
                        m[1] + m[True] + m["x"];
                        """,
                PeelValue.integer(60)
        );
    }
}
