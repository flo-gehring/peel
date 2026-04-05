package de.flogehring.peel.run;

import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.PeelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static de.flogehring.peel.run.TestHelpers.expectErrorOnRun;
import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;

public class ArithmeticTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            value = {
                    "Basic Addition,1+1;,2",
                    "Basic Subtraction,5-2;,3",
                    "Basic Multiplication,2*3;,6",
                    "Modulo,10%4;,2",
                    "Power,2**3;,8",
                    "Addition with unary minus,1+-2;,-1"
            }, delimiter = ','
    )
    void arithmeticCore(String testReason, String program, int expected) {
        runProgrammAndExpect(program, PeelValue.integer(expected));
    }

    @Test
    void floatingDivisionByDefault() {
        runProgrammAndExpect("1 / 2;", new Number.Float(0.5f));
    }

    @Test
    void precedenceMultiplicationBeforeAddition() {
        runProgrammAndExpect("1 + 2 * 3;", PeelValue.integer(7));
    }

    @Test
    void precedencePowerBeforeMultiplication() {
        runProgrammAndExpect("2 * 2 ** 3;", PeelValue.integer(16));
    }

    @Test
    void powerIsRightAssociative() {
        runProgrammAndExpect("2 ** 3 ** 2;", PeelValue.integer(512));
    }

    @Test
    void unaryMinusBindsWeakerThanPower() {
        runProgrammAndExpect("-2 ** 2;", PeelValue.integer(-4));
    }

    @Test
    void parensChangeUnaryMinusPowerBinding() {
        runProgrammAndExpect("(-2) ** 2;", PeelValue.integer(4));
    }

    @Test
    void decimalLiteralParsesAndEvaluatesAsDecimal() {
        runProgrammAndExpect("100.00;", new Number.Decimal(new BigDecimal("100.00")));
    }

    @Test
    void decimalArithmeticProducesDecimal() {
        runProgrammAndExpect("100.00 + 2;", new Number.Decimal(new BigDecimal("102.00")));
    }

    @Test
    void decimalMultiplicationProducesDecimal() {
        runProgrammAndExpect("1.50 * 2;", new Number.Decimal(new BigDecimal("3.00")));
    }

    @Test
    void moduloByZeroThrows() {
        expectErrorOnRun("1 % 0;", ArithmeticException.class);
    }

    @Test
    void divisionByZeroThrows() {
        expectErrorOnRun("1 / 0;", ArithmeticException.class);
    }

    @Test
    void negativeExponentFallsBackToFloat() {
        runProgrammAndExpect("2 ** -1;", new Number.Float(0.5f));
    }

    @Test
    void booleanOperatorsRespectPrecedence() {
        runProgrammAndExpect("True || False && False;", PeelValue.bool(true));
    }

    @Test
    void xorLowerPrecedenceThanAndAndHigherThanOr() {
        runProgrammAndExpect("True ^ True && False || False;", PeelValue.bool(true));
    }

    @Test
    void equalityAndNotEqualWork() {
        runProgrammAndExpect("1 != 2;", PeelValue.bool(true));
        runProgrammAndExpect("1 == 1;", PeelValue.bool(true));
    }

    @Test
    void shortCircuitAndSkipsRhs() {
        runProgrammAndExpect("False && (1 / 0 == 0);", PeelValue.bool(false));
    }

    @Test
    void shortCircuitOrSkipsRhs() {
        runProgrammAndExpect("True || (1 / 0 == 0);", PeelValue.bool(true));
    }

    @Test
    void boolLiteralCaseSensitivity() {
        expectErrorOnRun("true;", PeelException.class);
    }
}
