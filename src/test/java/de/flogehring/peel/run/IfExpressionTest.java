package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.lang.Program;
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
        void ifWithTrueConditionExecutesThenBlock() {
            Program program = PeelGrammar.parse("""
                        x = 0;
                        if (1 == 1) {
                            x = 5;
                        }
                        x;
                    """);
            SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
            EvaluatedProgram result = runtime.run(program);
            assertThat(result.getLastExpression().value()).isEqualTo(integer(5));
        }

        @Test
        void ifWithFalseConditionSkipsThenBlock() {
            Program program = PeelGrammar.parse("""
                        x = 0;
                        if (1 == 2) {
                            x = 5;
                        }
                        x;
                    """);
            SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
            EvaluatedProgram result = runtime.run(program);
            assertThat(result.getLastExpression().value()).isEqualTo(integer(0));
        }

        @Test
        void ifWithNestedConditions() {
            Program program = PeelGrammar.parse("""
                        x = 0;
                        if (1 == 1) {
                            if (2 == 2) {
                                x = 10;
                            }
                        }
                        x;
                    """);
            SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
            EvaluatedProgram result = runtime.run(program);
            assertThat(result.getLastExpression().value()).isEqualTo(integer(10));
        }

        @Test
        void ifExpressionReturnsLastValue() {
            Program program = PeelGrammar.parse("""
                        x = if (1 == 1) {
                            5;
                        };
                        x;
                    """);
            SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
            EvaluatedProgram result = runtime.run(program);
            assertThat(result.getLastExpression().value()).isEqualTo(integer(5));
        }

        @Test
        void ifExpressionWithMultipleStatementsReturnsLast() {
            Program program = PeelGrammar.parse("""
                        x = if (1 == 1) {
                            y = 3;
                            10;
                        };
                        x;
                    """);
            SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
            EvaluatedProgram result = runtime.run(program);
            assertThat(result.getLastExpression().value()).isEqualTo(integer(10));
        }

        @Test
        void conditionMustBeBooleanType() {
            Program program = PeelGrammar.parse("""
                        if (5) {
                            x = 1;
                        }
                    """);
            SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> runtime.run(program))
                    .withMessageContaining("condition must be Bool");
        }
    }
}
