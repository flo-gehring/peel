package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;
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
            SimpleRuntime simpleRuntime = RuntimeFactory.defaultLanguage();
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
            SimpleRuntime simpleRuntime = RuntimeFactory.defaultLanguage();
            Program parse = PeelGrammar.parse(
                    """
                            x = 1;
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
                        x = 0;
                        if (1 == 1) {
                            x = 5;
                        }
                        x;
                    """, integer(5));
        }

        @Test
        void ifWithFalseConditionSkipsThenBlock() {
            runProgrammAndExpect("""
                        x = 0;
                        if (1 == 2) {
                            x = 5;
                        }
                        x;
                    """, integer(0));
        }

        @Test
        void ifWithNestedConditions() {
            runProgrammAndExpect("""
                        x = 0;
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
                        x = if (1 == 1) {
                            5;
                        };
                        x;
                    """, integer(5));
        }

        @Test
        void ifExpressionWithMultipleStatementsReturnsLast() {
            String program1 = """
                        x = if (1 == 1) {
                            y = 3;
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
                            x = 1;
                        }
                    """);
            SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
            assertThatExceptionOfType(PeelException.class)
                    .isThrownBy(() -> runtime.run(program))
                    .withMessageContaining("condition must be Bool");
        }

        @Test
        void ifWithoutElseWhenConditionFalse() {
            runProgrammAndExpect("""
                        x = if (1 == 2) {
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
                        x = if (1 == 1) {
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
                        result = if (1 == 2) {
                            100;
                        } else {
                        };
                        result;
                    """, None.NONE);
        }

        @Test
        void ifConditionWithComplexExpression() {
            runProgrammAndExpect("""
                        x = 5;
                        y = 3;
                        sum = x + y;
                        result = if (sum == 8) {
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
                        result = 5;
                        y = 1;
                        x = 1;
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
                        result = 5;
                        y = 2;
                        x = 1;
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
                        result = 5;
                        y = 1;
                        x = 2;
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
                        result = 5;
                        y = 2;
                        x = 1;
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
                        x = 3;
                        result = if (x == 1) {
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
                        x = 1;
                        y = 2;
                        result = if (x == 1) {
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
                        x = 5;
                        result = if (x == 5) {
                            a = 10;
                            b = 20;
                            a + b;
                        } else {
                            a = 1;
                            b = 2;
                            a * b;
                        };
                        result;
                    """, integer(30));
        }

        @Test
        void ifElseIfWithoutFinalElseWhenNoMatch() {
            runProgrammAndExpect("""
                        x = if (5 == 1) {
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
                        x = 10;
                        result = if (x == 1) {
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
                        x = if (1 == 1) {
                        };
                        x;
                    """, None.NONE);
        }

        @Test
        void emptyElseBlockReturnsNone() {
            runProgrammAndExpect("""
                        x = if (1 == 2) {
                            10;
                        } else {
                        };
                        x;
                    """, None.NONE);
        }

        @Test
        void emptyBlockInElseIfChain() {
            runProgrammAndExpect("""
                        x = if (1 == 2) {
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
                        x = 0;
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
                        result = 0;
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
                        x = 10;
                        result = if (x == 5) {
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
