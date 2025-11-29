package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    }
}
