package de.flogehring.peel.run;

import de.flogehring.peel.parse.RedeclaredVariableException;
import de.flogehring.peel.run.exceptions.UndefinedVarException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.flogehring.peel.core.values.PeelValue.integer;
import static de.flogehring.peel.run.TestHelpers.*;

/**
 * This test covers the Scoping rules of Peel. It validates
 * that
 * - no variables in leak from their blocks
 * - scoping works for "for"-Loops
 * - you can't redeclare variables in the same scope
 * - shadowing of variables works
 * - you can't assign to undeclared variables
 * - you can't use variables in their own declaration
 * - you can use variables in subsequent assignments to themselves
 */
public class ScopingTest {

    @Nested
    class Parsing {

        @Test
        void redeclaringVariablesInSameScopeIsForbidden() {
            String programm = """
                    var i = 5;
                    var i = 2;
                    """;
            expectErrorOnParse(programm, RedeclaredVariableException.class);
        }


        @Test
        void reassignmentWorks() {
            String programm = """
                    var i = 5;
                     i = 2;
                    """;
            runProgrammAndExpect(programm, integer(2));
        }
    }

    @Nested
    class Runtime {

        @Test
        void noBlockLeakage() {
            String programm = """
                    var i = 5;
                    if(1 == 1){
                        var s = 4;
                    }
                    i + s;
                    """;
            expectErrorOnRun(programm, UndefinedVarException.class);
        }

        @Test
        void redeclaringInInnerBlockWorks() {
            String programm = """
                    var willBeShadowed = 5;
                    var i = 1;
                    if (i == 1) {
                        var willBeShadowed = 1;
                        willBeShadowed = willBeShadowed  + 1;
                    }
                    else {
                        willBeShadowed = 10;
                    }
                    willBeShadowed;
                    """;
            runProgrammAndExpect(programm, integer(5));
        }

        @Test
        void shadowingWorks() {
            String programm = """
                    var willBeShadowed = 5;
                    var i = 2;
                    if (i == 1) {
                        var willBeShadowed = 1;
                        willBeShadowed = willBeShadowed  + 1;
                    }
                    else {
                        willBeShadowed = 10;
                    }
                    willBeShadowed;
                    """;
            runProgrammAndExpect(programm, integer(10));
        }
    }
}
