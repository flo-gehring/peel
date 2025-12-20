package de.flogehring.peel.run;

import de.flogehring.peel.parse.AssignmentToUndeclaredVariable;
import de.flogehring.peel.parse.RedeclaredVariableException;
import de.flogehring.peel.parse.UninitializedVarExpression;
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

        @Nested
        class BlockScoping {
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

            @Test
            void deeplyNestedScopesWork() {
                String programm = """
                        var a = 1;
                        if (1 == 1) {
                            var b = 2;
                            if (2 == 2) {
                                var c = 3;
                                if (3 == 3) {
                                    var d = 4;
                                    a = a + b + c + d;
                                }
                            }
                        }
                        a;
                        """;
                runProgrammAndExpect(programm, integer(10));
            }

            @Test
            void variablesDontLeakFromElseBranch() {
                String programm = """
                        if (1 == 2) {
                            var x = 5;
                        } else {
                            var y = 10;
                        }
                        y;
                        """;
                expectErrorOnRun(programm, UndefinedVarException.class);
            }
        }

        @Nested
        class ForLoopScoping {
            @Test
            void forLoopIteratorVariableDoesNotLeak() {
                String programm = """
                        for (i in [1, 2, 3]) {
                            i;
                        }
                        i;
                        """;
                expectErrorOnRun(programm, UndefinedVarException.class);
            }

            @Test
            void forLoopBodyVariablesDoNotLeak() {
                String programm = """
                        for (i in [1, 2, 3]) {
                            var temp = i + 1;
                        }
                        temp;
                        """;
                expectErrorOnRun(programm, UndefinedVarException.class);
            }

            @Test
            void forLoopCanAccessOuterScopeVariables() {
                String programm = """
                        var sum = 0;
                        for (i in [1, 2, 3]) {
                            sum = sum + i;
                        }
                        sum;
                        """;
                runProgrammAndExpect(programm, integer(6));
            }

            @Test
            void forLoopCanShadowOuterVariable() {
                String programm = """
                        var i = 100;
                        for (i in [1, 2, 3]) {
                            i;
                        }
                        i;
                        """;
                runProgrammAndExpect(programm, integer(100));
            }

            @Test
            void nestedForLoopsWithShadowing() {
                String programm = """
                        var result = 0;
                        for (i in [1, 2]) {
                            for (i in [10, 20]) {
                                result = i;
                            }
                        }
                        result;
                        """;
                runProgrammAndExpect(programm, integer(20));
            }

            @Test
            void nestedForLoopInnerVariableDoesNotLeak() {
                String programm = """
                        for (i in [1, 2]) {
                            for (j in [10, 20]) {
                                j;
                            }
                            j;
                        }
                        """;
                expectErrorOnRun(programm, UndefinedVarException.class);
            }
        }

        @Nested
        class UndeclaredVariables {
            @Test
            void cannotAssignToUndeclaredVariable() {
                String programm = """
                        x = 5;
                        """;
                expectErrorOnParse(programm, AssignmentToUndeclaredVariable.class);
            }

            @Test
            void cannotAssignToUndeclaredVariableInBlock() {
                String programm = """
                        if (1 == 1) {
                            y = 10;
                        }
                        """;
                expectErrorOnParse(programm, AssignmentToUndeclaredVariable.class);
            }

            @Test
            void cannotAssignToVariableDeclaredInDifferentScope() {
                String programm = """
                        if (1 == 1) {
                            var z = 5;
                        }
                        z = 10;
                        """;
                expectErrorOnParse(programm, AssignmentToUndeclaredVariable.class);
            }

            @Test
            void cannotUseUndeclaredVariableInExpression() {
                String programm = """
                        var x = 5;
                        x + undeclared;
                        """;
                expectErrorOnRun(programm, UndefinedVarException.class);
            }
        }

        @Nested
        class SelfReference {
            @Test
            void cannotUseVariableInOwnDeclaration() {
                String programm = """
                        var x = x + 1;
                        """;
                expectErrorOnParse(programm, UninitializedVarExpression.class);
            }

            @Test
            void cannotUseVariableInOwnDeclarationEvenWithOuterScope() {
                String programm = """
                        var x = 5;
                        if (1 == 1) {
                            var x = x + 1;
                        }
                        """;
                expectErrorOnParse(programm, UninitializedVarExpression.class);
            }

            @Test
            void canUseVariableInSubsequentAssignmentToItself() {
                String programm = """
                        var x = 5;
                        x = x + 1;
                        x;
                        """;
                runProgrammAndExpect(programm, integer(6));
            }

            @Test
            void canUseOuterVariableInInnerScopeAssignment() {
                String programm = """
                        var x = 5;
                        if (1 == 1) {
                            x = x + 1;
                        }
                        x;
                        """;
                runProgrammAndExpect(programm, integer(6));
            }
        }

        @Nested
        class MultipleShadowingLevels {
            @Test
            void threeLevelsOfShadowing() {
                String programm = """
                        var x = 1;
                        if (1 == 1) {
                            var x = 2;
                            if (2 == 2) {
                                var x = 3;
                                x;
                            }
                        }
                        """;
                runProgrammAndExpect(programm, integer(3));
            }

            @Test
            void shadowingResolvesCorrectlyAfterBlockExit() {
                String programm = """
                        var x = 1;
                        if (1 == 1) {
                            var x = 2;
                            if (2 == 2) {
                                var x = 3;
                            }
                            x;
                        }
                        """;
                runProgrammAndExpect(programm, integer(2));
            }

            @Test
            void outerVariableUnchangedAfterShadowing() {
                String programm = """
                        var x = 1;
                        if (1 == 1) {
                            var x = 2;
                            if (2 == 2) {
                                var x = 3;
                            }
                        }
                        x;
                        """;
                runProgrammAndExpect(programm, integer(1));
            }

            @Test
            void shadowingAcrossDifferentBlockTypes() {
                String programm = """
                        var x = 1;
                        if (1 == 1) {
                            var x = 2;
                            for (i in [1]) {
                                var x = 3;
                                x;
                            }
                        }
                        """;
                runProgrammAndExpect(programm, integer(3));
            }
        }

        @Nested
        class MixedScenarios {
            @Test
            void multipleVariablesInNestedScopes() {
                String programm = """
                        var a = 1;
                        var b = 2;
                        if (1 == 1) {
                            var c = 3;
                            a = a + c;
                            if (2 == 2) {
                                var d = 4;
                                b = b + d;
                            }
                        }
                        a + b;
                        """;
                runProgrammAndExpect(programm, integer(10));
            }

            @Test
            void forLoopWithIfStatement() {
                String programm = """
                        var result = 0;
                        for (i in [1, 2, 3]) {
                            if (i == 2) {
                                var matched = 100;
                                result = matched;
                            }
                        }
                        result;
                        """;
                runProgrammAndExpect(programm, integer(100));
            }

            @Test
            void variableFromIfDoesNotLeakToForLoop() {
                String programm = """
                        if (1 == 1) {
                            var x = 5;
                        }
                        for (i in [1]) {
                            x;
                        }
                        """;
                expectErrorOnRun(programm, UndefinedVarException.class);
            }

            @Test
            void complexShadowingAndAssignment() {
                String programm = """
                        var x = 1;
                        var y = 10;
                        if (1 == 1) {
                            var x = 2;
                            y = y + x;
                            for (i in [1]) {
                                var x = 3;
                                y = y + x;
                            }
                            y = y + x;
                        }
                        x + y;
                        """;
                runProgrammAndExpect(programm, integer(18));
            }
        }
    }
}
