package de.flogehring.peel.run;

import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.core.values.Primitives;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
                PeelValue.Collection.peelMap(Map.<Primitives, PeelValue>of(
                        PeelValue.integer(1), PeelValue.integer(10),
                        (Primitives) PeelValue.bool(true), PeelValue.integer(20),
                        PeelValue.text("x"), PeelValue.integer(30)
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
