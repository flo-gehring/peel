package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peel.run.exceptions.PeelException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.flogehring.peel.core.values.PeelValue.integer;
import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class IfExpressionTest {

    @Nested
    class SimpleExpression {

        @Test
        void numberComparison() {
            Runtime simpleRuntime = RuntimeFactory.defaultLanguage();
            Program parse = PeelGrammar.parse(
                    """
                            if(0 == 1) {
                                1;
                            } else {
                                2;
                            }
                            """
            );
            EvaluatedProgram run = simpleRuntime.run(parse);
            assertThat(run.getLastExpression().value()).isEqualTo(PeelValue.integer(2));
        }

        @Test
        void varComparison() {
            Runtime simpleRuntime = RuntimeFactory.defaultLanguage();
            Program parse = PeelGrammar.parse(
                    """
                            var x = 1;
                            if(x == 1) {
                                1;
                            } else {
                                2;
                            }
                            """
            );
            EvaluatedProgram run = simpleRuntime.run(parse);
            assertThat(run.getLastExpression().value()).isEqualTo(PeelValue.integer(1));
        }

        @Test
        void ifWithTrueConditionExecutesThenBlock() {
            runProgrammAndExpect("""
                        var x = 0;
                        if (1 == 1) {
                            x = 5;
                        }
                        x;
                    """, integer(5));
        }

        @Test
        void ifWithFalseConditionSkipsThenBlock() {
            runProgrammAndExpect("""
                        var x = 0;
                        if (1 == 2) {
                            x = 5;
                        }
                        x;
                    """, integer(0));
        }

        @Test
        void ifWithNestedConditions() {
            runProgrammAndExpect("""
                        var x = 0;
                        if (1 == 1) {
                            if (2 == 2) {
                                x = 10;
                            }
                        }
                        x;
                    """, integer(10));
        }

        @Test
        void ifExpressionReturnsLastValue() {
            runProgrammAndExpect("""
                        var x = if (1 == 1) {
                            5;
                        };
                        x;
                    """, integer(5));
        }

        @Test
        void ifExpressionWithMultipleStatementsReturnsLast() {
            String program1 = """
                        var x = if (1 == 1) {
                            var y = 3;
                            10;
                        };
                        x;
                    """;
            Number.Integer integer = integer(10);
            runProgrammAndExpect(program1, integer);
        }

        @Test
        void conditionMustBeBooleanType() {
            Program program = PeelGrammar.parse("""
                        if (5) {
                            var x = 1;
                        }
                    """);
            Runtime runtime = RuntimeFactory.defaultLanguage();
            assertThatExceptionOfType(PeelException.class)
                    .isThrownBy(() -> runtime.run(program))
                    .withMessageContaining("condition must be Bool");
        }

        @Test
        void ifWithoutElseWhenConditionFalse() {
            runProgrammAndExpect("""
                        var x = if (1 == 2) {
                            10;
                        };
                        x;
                    """, None.NONE);
        }

        @Test
        void ifWithoutElseReturnsNoneWhenFalse() {
            runProgrammAndExpect("""
                        if (5 == 10) {
                            42;
                        }
                    """, None.NONE);
        }

        @Test
        void ifWithoutElseReturnsValueWhenTrue() {
            runProgrammAndExpect("""
                        if (5 == 5) {
                            42;
                        }
                    """, integer(42));
        }

        @Test
        void ifElseBranchesWithDifferentTypes() {
            runProgrammAndExpect("""
                        var x = if (1 == 1) {
                            42;
                        } else {
                            "text";
                        };
                        x;
                    """, integer(42));
        }

        @Test
        void ifElseReturningNoneAndInteger() {
            runProgrammAndExpect("""
                        var result = if (1 == 2) {
                            100;
                        } else {
                        };
                        result;
                    """, None.NONE);
        }

        @Test
        void ifConditionWithComplexExpression() {
            runProgrammAndExpect("""
                        var x = 5;
                        var y = 3;
                        var sum = x + y;
                        var result = if (sum == 8) {
                            100;
                        } else {
                            0;
                        };
                        result;
                    """, integer(100));
        }
    }

    @Nested
    class MultipleIfElse {

        @Test
        void ifTaken() {
            runProgrammAndExpect("""
                        var result = 5;
                        var y = 1;
                        var x = 1;
                        if (x == y) {
                            result = 1;
                        } else if (x == 1) {
                            result = 2;
                        } else {
                            result = 3;
                        }
                        result;
                    """, integer(1));
        }

        @Test
        void firstElseIfTaken() {
            runProgrammAndExpect("""
                        var result = 5;
                        var y = 2;
                        var x = 1;
                        if (x == y) {
                            x = 2;
                            result = 1;
                        } else if (x == 1) {
                            result = 2;
                        }
                       else if (y == 1) {
                            result = 3;
                        }
                        else {
                            result = 4;
                        }
                        result;
                    """, integer(2));
        }

        @Test
        void secondElseIfTaken() {
            runProgrammAndExpect("""
                        var result = 5;
                        var y = 1;
                        var x = 2;
                        if (x == y) {
                            x = 1;
                            result = 1;
                        } else if (x == 1) {
                            result = 2;
                        }
                       else if (y == 1) {
                            result = 3;
                        }
                        else {
                            result = 4;
                        }
                        result;
                    """, integer(3));
        }


        @Test
        void elseTaken() {
            runProgrammAndExpect("""
                        var result = 5;
                        var y = 2;
                        var x = 1;
                        if (x == y) {
                            result = 1;
                        } else if (x == 2) {
                            result = 2;
                        } else {
                            result = 3;
                        }
                        result;
                    """, integer(3));
        }

        @Test
        void ifElseChainAsAssignment() {
            runProgrammAndExpect("""
                        var x = 3;
                        var result = if (x == 1) {
                            100;
                        } else if (x == 2) {
                            200;
                        } else if (x == 3) {
                            300;
                        } else {
                            400;
                        };
                        result;
                    """, integer(300));
        }

        @Test
        void nestedIfElseAsAssignment() {
            runProgrammAndExpect("""
                        var x = 1;
                        var y = 2;
                        var result = if (x == 1) {
                            if (y == 2) {
                                42;
                            } else {
                                21;
                            }
                        } else {
                            0;
                        };
                        result;
                    """, integer(42));
        }

        @Test
        void ifElseWithMultipleStatementsInBranches() {
            runProgrammAndExpect("""
                        var x = 5;
                        var result = if (x == 5) {
                            var a = 10;
                            var b = 20;
                            a + b;
                        } else {
                            var a = 1;
                            var b = 2;
                            a * b;
                        };
                        result;
                    """, integer(30));
        }

        @Test
        void ifElseIfWithoutFinalElseWhenNoMatch() {
            runProgrammAndExpect("""
                        var x = if (5 == 1) {
                            1;
                        } else if (5 == 2) {
                            2;
                        };
                        x;
                    """, None.NONE);
        }

        @Test
        void allBranchesSkipped() {
            runProgrammAndExpect("""
                        var x = 10;
                        var result = if (x == 1) {
                            1;
                        } else if (x == 2) {
                            2;
                        } else if (x == 3) {
                            3;
                        };
                        result;
                    """, None.NONE);
        }

    }

    @Nested
    class EmptyBlocks {

        @Test
        void emptyThenBlockReturnsNone() {
            runProgrammAndExpect("""
                        var x = if (1 == 1) {
                        };
                        x;
                    """, None.NONE);
        }

        @Test
        void emptyElseBlockReturnsNone() {
            runProgrammAndExpect("""
                        var x = if (1 == 2) {
                            10;
                        } else {
                        };
                        x;
                    """, None.NONE);
        }

        @Test
        void emptyBlockInElseIfChain() {
            runProgrammAndExpect("""
                        var x = if (1 == 2) {
                            1;
                        } else if (2 == 2) {
                        } else {
                            3;
                        };
                        x;
                    """, None.NONE);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void multipleIndependentIfStatements() {
            runProgrammAndExpect("""
                        var x = 0;
                        if (1 == 1) {
                            x = 1;
                        }
                        if (2 == 2) {
                            x = 2;
                        }
                        x;
                    """, integer(2));
        }

        @Test
        void deeplyNestedIfStatements() {
            runProgrammAndExpect("""
                        var result = 0;
                        if (1 == 1) {
                            if (2 == 2) {
                                if (3 == 3) {
                                    if (4 == 4) {
                                        result = 42;
                                    }
                                }
                            }
                        }
                        result;
                    """, integer(42));
        }

        @Test
        void nestedIfElseInElseBranch() {
            runProgrammAndExpect("""
                        var x = 10;
                        var result = if (x == 5) {
                            1;
                        } else {
                            if (x == 10) {
                                2;
                            } else {
                                3;
                            }
                        };
                        result;
                    """, integer(2));
        }
    }
}
