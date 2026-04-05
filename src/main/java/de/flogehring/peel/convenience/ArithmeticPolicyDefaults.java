package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.ArithmeticConfiguration;
import de.flogehring.peel.core.eval.ArithmeticPolicy;

import java.math.RoundingMode;

public final class ArithmeticPolicyDefaults {

    private ArithmeticPolicyDefaults() {
    }

    public static ArithmeticPolicy standard() {
        return ArithmeticConfiguration.init()
                .treatDecimalAsJavaBigDecimal()
                .mixedTypePromotion(ArithmeticConfiguration.MixedTypePromotion.WIDEST)
                .onDivision(ArithmeticConfiguration.DivisionRule.keepFloat())
                .divisionByZero(ArithmeticConfiguration.DivisionByZeroPolicy.THROW)
                .build();
    }

    public static ArithmeticPolicy fast() {
        return ArithmeticConfiguration.init()
                .treatDecimalAsFloat()
                .mixedTypePromotion(ArithmeticConfiguration.MixedTypePromotion.FLOAT_PREFERRED)
                .onDivision(ArithmeticConfiguration.DivisionRule.keepFloat())
                .divisionByZero(ArithmeticConfiguration.DivisionByZeroPolicy.FLOAT_NAN)
                .build();
    }

    public static ArithmeticPolicy financial() {
        return ArithmeticConfiguration.init()
                .treatDecimalAsJavaBigDecimal()
                .mixedTypePromotion(ArithmeticConfiguration.MixedTypePromotion.DECIMAL_PREFERRED)
                .onDivision(ArithmeticConfiguration.DivisionRule.decimal(2, RoundingMode.HALF_UP))
                .divisionByZero(ArithmeticConfiguration.DivisionByZeroPolicy.THROW)
                .build();
    }
}
