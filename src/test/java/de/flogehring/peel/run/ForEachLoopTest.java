package de.flogehring.peel.run;

import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.PeelValue;
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

    @Test
    @Timeout(2)
    void listWithLiteralsEmptyBody() {
        runProgrammAndExpect(
                """
                          for (x in [1, 2, 3]) {}
                        """,
                None.NONE
        );
    }

    @Test
    @Timeout(2)
    void listWithLiteralsSingleStatement() {
        runProgrammAndExpect(
                """
                          var sum = 0;
                          for (x in [1, 2, 3]) {
                              sum = sum + x;
                          }
                          sum;
                        """,
                PeelValue.integer(6)
        );
    }

    @Test
    @Timeout(2)
    void listWithExpressionsEmptyBody() {
        runProgrammAndExpect(
                """
                          for (x in [1 + 1, 2 * 3, 10 - 5]) {}
                        """,
                None.NONE
        );
    }

    @Test
    @Timeout(2)
    void listWithExpressionsSumming() {
        runProgrammAndExpect(
                """
                          var sum = 0;
                          for (x in [1 + 1, 2 * 3, 10 - 5]) {
                              sum = sum + x;
                          }
                          sum;
                        """,
                PeelValue.integer(13)  // 2 + 6 + 5
        );
    }

    @Test
    @Timeout(2)
    void loopBodyWithMultipleStatements() {
        runProgrammAndExpect(
                """
                          var sum = 0;
                          var product = 1;
                          for (x in [2, 3, 4]) {
                              sum = sum + x;
                              product = product * x;
                          }
                          product;
                        """,
                PeelValue.integer(24)  // 2 * 3 * 4
        );
    }

    @Test
    @Timeout(2)
    void loopVariableInExpression() {
        runProgrammAndExpect(
                """
                          var result = 0;
                          for (x in [1, 2, 3]) {
                              result = result + (x * 2);
                          }
                          result;
                        """,
                PeelValue.integer(12)  // (1*2) + (2*2) + (3*2)
        );
    }

    @Test
    @Timeout(2)
    void nestedForEachLoops() {
        runProgrammAndExpect(
                """
                          var sum = 0;
                          for (i in [1, 2]) {
                              for (j in [10, 20]) {
                                  sum = sum + (i * j);
                              }
                          }
                          sum;
                        """,
                PeelValue.integer(90)  // (1*10) + (1*20) + (2*10) + (2*20)
        );
    }

    @Test
    @Timeout(2)
    void singleElementList() {
        runProgrammAndExpect(
                """
                          var result = 0;
                          for (x in [42]) {
                              result = x;
                          }
                          result;
                        """,
                PeelValue.integer(42)
        );
    }

    @Test
    @Timeout(2)
    void emptyListDoesNotModifyVariable() {
        runProgrammAndExpect(
                """
                          var x = 99;
                          for (x in []) {
                              x = 0;
                          }
                          x;
                        """,
                PeelValue.integer(99)  // x should remain unchanged
        );
    }

    @Test
    @Timeout(2)
    void loopReturnsLastIterationValue() {
        runProgrammAndExpect(
                """
                          for (x in [5, 10, 15]) {
                              x * 2;
                          }
                        """,
                PeelValue.integer(30)  // Last iteration: 15 * 2
        );
    }
}
