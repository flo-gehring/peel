package de.flogehring.peel.run;

import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.UndefinedVarException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static de.flogehring.peel.core.values.PeelValue.integer;
import static de.flogehring.peel.run.TestHelpers.expectErrorOnRun;
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

    @Nested
    class Scoping {

        @Test
        @Timeout(2)
        void variablesDeclaredInWhileLoopDoNotLeak() {
            String programm = """
                    var x = 0;
                    while (!(x == 3)) {
                        var temp = x + 1;
                        x = temp;
                    }
                    temp;
                    """;
            expectErrorOnRun(programm, UndefinedVarException.class);
        }

        @Test
        @Timeout(2)
        void whileLoopCanModifyOuterScopeVariables() {
            runProgrammAndExpect(
                    """
                                var sum = 0;
                                var counter = 0;
                                while (!(counter == 5)) {
                                    sum = sum + counter;
                                    counter = counter + 1;
                                }
                                sum;
                            """,
                    integer(10)
            );
        }

        @Test
        @Timeout(2)
        void whileLoopCanShadowOuterVariable() {
            runProgrammAndExpect(
                    """
                                var x = 100;
                                var counter = 0;
                                while (!(counter == 3)) {
                                    var x = counter;
                                    counter = counter + 1;
                                }
                                x;
                            """,
                    integer(100)
            );
        }

        @Test
        @Timeout(2)
        void nestedWhileLoopsWithShadowing() {
            runProgrammAndExpect(
                    """
                                var result = 0;
                                var i = 0;
                                while (!(i == 2)) {
                                    var j = 0;
                                    while (!(j == 2)) {
                                        var i = 10;
                                        result = i + j;
                                        j = j + 1;
                                    }
                                    i = i + 1;
                                }
                                result;
                            """,
                    integer(11)
            );
        }

        @Test
        @Timeout(2)
        void nestedWhileLoopInnerVariableDoesNotLeak() {
            String programm = """
                    var i = 0;
                    while (!(i == 2)) {
                        var j = 0;
                        while (!(j == 2)) {
                            var innerVar = 42;
                            j = j + 1;
                        }
                        i = i + 1;
                    }
                    innerVar;
                    """;
            expectErrorOnRun(programm, UndefinedVarException.class);
        }

        @Test
        @Timeout(2)
        void whileLoopBodyBlockCreatesScope() {
            String programm = """
                    var x = 0;
                    while (!(x == 2)) {
                        var blockVar = 5;
                        x = x + 1;
                    }
                    blockVar;
                    """;
            expectErrorOnRun(programm, UndefinedVarException.class);
        }

        @Test
        @Timeout(2)
        void whileLoopWithIfStatementScoping() {
            runProgrammAndExpect(
                    """
                                var result = 0;
                                var counter = 0;
                                while (!(counter == 3)) {
                                    if (counter == 1) {
                                        var matched = 100;
                                        result = matched;
                                    }
                                    counter = counter + 1;
                                }
                                result;
                            """,
                    integer(100)
            );
        }

        @Test
        @Timeout(2)
        void whileLoopConditionCanAccessOuterVariables() {
            runProgrammAndExpect(
                    """
                                var limit = 5;
                                var counter = 0;
                                while (!(counter == limit)) {
                                    counter = counter + 1;
                                }
                                counter;
                            """,
                    integer(5)
            );
        }

        @Test
        @Timeout(2)
        void multipleVariablesInWhileLoopScope() {
            String programm = """
                    var x = 0;
                    while (!(x == 2)) {
                        var a = 1;
                        var b = 2;
                        var c = 3;
                        x = x + 1;
                    }
                    a;
                    """;
            expectErrorOnRun(programm, UndefinedVarException.class);
        }
    }
}
