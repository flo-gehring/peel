package de.flogehring.peel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class SimpleRuntimeTest {


    @Test
    void simpleTest() {
        Programm p = new Programm(List.of(
                CodeElement.assign("x", CodeElement.literal(1)),
                CodeElement.assign("y", CodeElement.literal(1)),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        Assertions.assertEquals(2.0, runtime.run(p));
    }
}
