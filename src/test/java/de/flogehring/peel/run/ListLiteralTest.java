package de.flogehring.peel.run;

import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.PeelException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.flogehring.peel.run.TestHelpers.expectErrorOnRun;
import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;

public class ListLiteralTest {

    @Test
    void numberList() {
        runProgrammAndExpect(
                """
                          [1,2,3,]
                        """,
                PeelValue.list(List.of(PeelValue.integer(1), PeelValue.integer(2), PeelValue.integer(3)))
        );
    }

    @Test
    void listSelectorSimple() {
        runProgrammAndExpect(
                """
                        var l = [1,2,3];
                        l[1];
                        """,
                PeelValue.integer(2)
        );
    }

    @Test
    void listSelectorInArithmeticExpression() {
        runProgrammAndExpect(
                """
                        var l = [1,2,3];
                        l[1] + l[2];
                        """,
                PeelValue.integer(5)
        );
    }

    @Test
    void listSelectorWithExpressionIndex() {
        runProgrammAndExpect(
                """
                        var l = [1,2,3,4];
                        l[1 + 1];
                        """,
                PeelValue.integer(3)
        );
    }

    @Test
    void listSelectorOutOfBoundsThrows() {
        expectErrorOnRun(
                """
                        var l = [1,2,3];
                        l[5];
                        """,
                PeelException.class
        );
    }

    @Test
    void listSelectorNegativeIndexThrows() {
        expectErrorOnRun(
                """
                        var l = [1,2,3];
                        l[-1];
                        """,
                PeelException.class
        );
    }

    @Test
    void listSelectorWithWrongIndexTypeThrows() {
        expectErrorOnRun(
                """
                        var l = [1,2,3];
                        l[False];
                        """,
                PeelException.class
        );
    }

    @Test
    void listWithExpressions() {
        runProgrammAndExpect(
                """
                        var l = [1 + 2, 2 ** 3, True || False];
                        l;
                        """,
                PeelValue.list(List.of(PeelValue.integer(3), PeelValue.integer(8), PeelValue.bool(true)))
        );
    }

    @Test
    void listWithExpressionsSelectorAccess() {
        runProgrammAndExpect(
                """
                        var l = [1 + 2, 2 ** 3, True || False];
                        l[0] + l[1];
                        """,
                PeelValue.integer(11)
        );
    }
}
