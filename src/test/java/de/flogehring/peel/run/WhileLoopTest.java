package de.flogehring.peel.run;

import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.PeelValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
public class WhileLoopTest {

    @Test
    @Timeout(2)
    void immediatelyExit() {
        runProgrammAndExpect(
                """
                            var x = 2;
                            var y = 2;
                            while (!(x == y)) {
                                x = x + 1;
                            }
                        """,
                None.NONE
        );
    }

    @Test
    @Timeout(2)
    void twoIterations() {
        runProgrammAndExpect(
                """
                            var x = 0;
                            var y = 2;
                            while (!(x == y)) {
                                x = x + 1;
                            }
                        """,
                PeelValue.integer(2)
        );
    }
}
