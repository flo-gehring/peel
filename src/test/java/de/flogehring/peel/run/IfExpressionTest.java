package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.flogehring.peel.core.values.PeelValue.integer;
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

    }

    private static void runProgrammAndExpect(String text, PeelValue expected) {
        Program program = PeelGrammar.parse(text);
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        EvaluatedProgram evaluated = runtime.run(program);
        assertThat(evaluated.getLastExpression().value()).isEqualTo(expected);
    }
}
