package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class TestHelpers {
    private TestHelpers() {

    }

    public static void runProgrammAndExpect(String text, PeelValue expected) {
        Program program = PeelGrammar.parse(text);
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(program);
        assertThat(trace.result()).isEqualTo(TraceValueMapper.fromPeelValue(expected));
    }

    public static void runProgrammAndExpect(String text, TraceValue expected) {
        Program program = PeelGrammar.parse(text);
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(program);
        assertThat(trace.result()).isEqualTo(expected);
    }

    public static void expectErrorOnRun(
            String text, Class<? extends RuntimeException> e
    ) {
        Program program = PeelGrammar.parse(text);
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        assertThatExceptionOfType(e).isThrownBy(() -> runtime.run(program));
    }

    public static void expectErrorOnParse(
            String text, Class<? extends RuntimeException> e
    ) {
        assertThatExceptionOfType(e).isThrownBy(() -> PeelGrammar.parse(text));
    }
}
