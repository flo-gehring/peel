package de.flogehring.peel.core.eval;

import de.flogehring.peel.convenience.ArithmeticPolicyDefaults;
import de.flogehring.peel.core.values.Number;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ArithmeticConfigurationTest {

    @Test
    void standardPolicySupportsBasicOperations() {
        ArithmeticPolicy policy = ArithmeticPolicyDefaults.standard();

        assertThat(policy.add(new Number.Integer(1), new Number.Integer(2))).isEqualTo(new Number.Integer(3));
        assertThat(policy.sub(new Number.Integer(5), new Number.Integer(2))).isEqualTo(new Number.Integer(3));
        assertThat(policy.mul(new Number.Integer(3), new Number.Integer(2))).isEqualTo(new Number.Integer(6));
        assertThat(policy.div(new Number.Integer(3), new Number.Integer(2))).isEqualTo(new Number.Float(1.5f));
        assertThat(policy.negate(new Number.Integer(7))).isEqualTo(new Number.Integer(-7));
    }

    @Test
    void financialPolicyUsesDecimalDivision() {
        ArithmeticPolicy policy = ArithmeticPolicyDefaults.financial();

        assertThat(policy.div(new Number.Integer(1), new Number.Integer(3)))
                .isEqualTo(new Number.Decimal(new java.math.BigDecimal("0.33")));
    }

    @Test
    void fastPolicyReturnsNanOnDivisionByZero() {
        ArithmeticPolicy policy = ArithmeticPolicyDefaults.fast();

        Number.Float value = (Number.Float) policy.div(new Number.Integer(10), new Number.Integer(0));
        assertThat(Float.isNaN(value.value())).isTrue();
    }

    @Test
    void floatNanRequiresKeepFloatDivision() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> ArithmeticConfiguration.init()
                        .divisionByZero(ArithmeticConfiguration.DivisionByZeroPolicy.FLOAT_NAN)
                        .onDivision(ArithmeticConfiguration.DivisionRule.castInt(RoundingMode.DOWN))
                        .build()
        );
    }

    @Test
    void decimalRuleRequiresBigDecimalBackend() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> ArithmeticConfiguration.init()
                        .treatDecimalAsFloat()
                        .onDivision(ArithmeticConfiguration.DivisionRule.decimal(2, RoundingMode.HALF_UP))
                        .build()
        );
    }

    @Test
    void roundOnDivisionWithoutCompatibleDivisionRuleThrows() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
                () -> ArithmeticConfiguration.init()
                        .roundOnDivision(RoundingMode.HALF_UP)
                        .build()
        );
    }

    @Test
    void divisionScaleWithoutDecimalRuleThrows() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
                () -> ArithmeticConfiguration.init()
                        .divisionScale(2)
                        .build()
        );
    }
}
