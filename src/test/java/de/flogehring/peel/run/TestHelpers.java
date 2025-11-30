package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestHelpers {
    private TestHelpers() {

    }

    public static void runProgrammAndExpect(String text, PeelValue expected) {
        Program program = PeelGrammar.parse(text);
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        EvaluatedProgram evaluated = runtime.run(program);
        assertThat(evaluated.getLastExpression().value()).isEqualTo(expected);
    }
}
