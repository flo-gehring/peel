package de.flogehring.peel.run;

import de.flogehring.peel.lang.CodeElement;
import de.flogehring.peel.lang.Program;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class SimpleRuntimeTest {


    @Test
    void simpleTest() {
        Program p = new Program(List.of(
                CodeElement.assign("x", CodeElement.literal(1)),
                CodeElement.assign("y", CodeElement.literal(1)),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        Assertions.assertEquals(2.0, runtime.run(p));
    }
}
