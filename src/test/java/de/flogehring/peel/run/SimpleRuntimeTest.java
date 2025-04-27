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

    @Test
    void simpleTestStrings() {
        Program p = new Program(List.of(
                CodeElement.assign("x", CodeElement.literal("1")),
                CodeElement.assign("y", CodeElement.literal("1")),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        Assertions.assertEquals("11", runtime.run(p));
    }

    @Test
    void simpleExternalAssignment() {
        Program p = new Program(List.of(
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        runtime.register(getVariable("x", "1"));
        runtime.register(getVariable("y", "2"));
        Assertions.assertEquals("12", runtime.run(p));
    }

    private static Variable getVariable(final String name, final String value) {
        return new Variable() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Object value() {
                return value;
            }
        };
    }
}