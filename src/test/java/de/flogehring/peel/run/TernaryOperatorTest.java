package de.flogehring.peel.run;

import de.flogehring.peel.core.values.PeelValue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.flogehring.peel.core.values.PeelValue.integer;
import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;

public class TernaryOperatorTest {

    @Test
    void assignmentFirstCase() {
        runProgrammAndExpect("1 == 1 ? 2: 3;",
                PeelValue.integer(2)
        );
    }

    @Test
    void assignmentSecondCase() {
        runProgrammAndExpect("1 == 2 ? 2: 3;",
                PeelValue.integer(3)
        );
    }

    @Test
    void ternaryPreventsDirectNestingInBranches() {

        runProgrammAndExpect("var x = 1 == 1 ? 5 : 10;", PeelValue.integer(5));
    }

    @Test
    void ternaryWithVariablesAndArithmetic() {
        runProgrammAndExpect("""
                    var a = 5;
                    var b = 10;
                    var result = a == 5 ? b + 1 : b - 1;
                    result;
                """, PeelValue.integer(11));
    }

    @Test
    void ternaryInArithmeticExpression() {
        runProgrammAndExpect("""
                    var x = 1;
                    var result = 10 + (x == 1 ? 5 : 0);
                    result;
                """, PeelValue.integer(15));
    }

    @Nested
    class Scoping {

        @Test
        void ternaryConditionCanAccessOuterVariables() {
            runProgrammAndExpect("""
                        var x = 5;
                        var y = 10;
                        var result = x == 5 ? y : 0;
                        result;
                    """, integer(10));
        }

        @Test
        void ternaryBranchesCanAccessOuterVariables() {
            runProgrammAndExpect("""
                        var a = 100;
                        var b = 200;
                        var result = 1 == 1 ? a : b;
                        result;
                    """, integer(100));
        }

        @Test
        void ternaryWithComplexExpressionsInBranches() {
            runProgrammAndExpect("""
                        var x = 5;
                        var y = 10;
                        var result = 1 == 1 ? x + y : x - y;
                        result;
                    """, integer(15));
        }

        @Test
        void ternaryWithVariablesFromDifferentScopes() {
            runProgrammAndExpect("""
                        var outer = 5;
                        if (1 == 1) {
                            var inner = 10;
                            var result = outer == 5 ? inner : 0;
                            result;
                        }
                    """, integer(10));
        }

        @Test
        void ternaryInLoopWithScopedVariables() {
            runProgrammAndExpect("""
                        var sum = 0;
                        for (i in [1, 2, 3, 4, 5]) {
                            var toAdd = i == 3 ? 100 : i;
                            sum = sum + toAdd;
                        }
                        sum;
                    """, integer(112));
        }

        @Test
        void ternaryAccessesVariablesFromOuterScopeInBothBranches() {
            runProgrammAndExpect("""
                        var trueValue = 100;
                        var falseValue = 200;
                        var condition = 1;
                        var result = condition == 1 ? trueValue : falseValue;
                        result;
                    """, integer(100));
        }

        @Test
        void ternaryInNestedIfStatement() {
            runProgrammAndExpect("""
                        var x = 5;
                        if (1 == 1) {
                            var y = 10;
                            var result = x == 5 ? y : 0;
                            result;
                        }
                    """, integer(10));
        }

        @Test
        void ternaryInWhileLoopCondition() {
            runProgrammAndExpect("""
                        var counter = 0;
                        var limit = 3;
                        var result = 0;
                        while (!(counter == (limit == 3 ? 3 : 5))) {
                            result = result + 1;
                            counter = counter + 1;
                        }
                        result;
                    """, integer(3));
        }

        @Test
        void ternaryWithVariableDeclarationInOuterScope() {
            runProgrammAndExpect("""
                        var x = 5;
                        var choice = 1;
                        var result = choice == 1 ? x + 10 : x - 10;
                        result;
                    """, integer(15));
        }
    }
}
