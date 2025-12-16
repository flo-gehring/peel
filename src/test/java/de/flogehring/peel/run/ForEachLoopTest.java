package de.flogehring.peel.run;

import de.flogehring.peel.core.values.None;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;

public class ForEachLoopTest {

    @Test
    @Timeout(2)
    void emptyListEmptyBody() {
        runProgrammAndExpect(
                """
                          for (x in []) {}
                        """,
                None.NONE
        );
    }


}
