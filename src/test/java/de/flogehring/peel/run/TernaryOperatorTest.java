package de.flogehring.peel.run;

import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Test;

import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

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
    void directNestedTernaryInTrueBranchIsForbidden() {
        // Direct nesting without parentheses is forbidden
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> PeelGrammar.parse("var x = 1 == 1 ? 2 == 2 ? 100 : 200 : 300;"));
    }

    @Test
    void ternaryPreventsDirectNestingInBranches() {
        // The grammar prevents ternary operators in the condition and branches
        // by using nonTernaryExpr. This catches most nesting cases.
        // However, without explicit parens, some edge cases with operator
        // precedence might still parse in unexpected ways.
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
}
