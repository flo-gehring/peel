package de.flogehring.peel.run;

import de.flogehring.peel.core.values.PeelValue;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
