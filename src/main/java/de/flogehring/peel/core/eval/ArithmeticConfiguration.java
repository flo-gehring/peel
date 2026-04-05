package de.flogehring.peel.core.eval;

import java.math.RoundingMode;
import java.util.Objects;

public final class ArithmeticConfiguration {

    public enum DecimalBackend {
        JAVA_BIG_DECIMAL,
        FLOAT
    }

    public enum MixedTypePromotion {
        WIDEST,
        DECIMAL_PREFERRED,
        FLOAT_PREFERRED
    }

    public enum DivisionByZeroPolicy {
        THROW,
        FLOAT_NAN
    }

    public sealed interface DivisionRule permits DivisionRule.KeepFloat, DivisionRule.CastInt, DivisionRule.DecimalResult {

        static DivisionRule keepFloat() {
            return new KeepFloat();
        }

        static DivisionRule castInt(RoundingMode roundingMode) {
            return new CastInt(Objects.requireNonNull(roundingMode, "roundingMode"));
        }

        static DivisionRule decimal(int scale, RoundingMode roundingMode) {
            return new DecimalResult(scale, Objects.requireNonNull(roundingMode, "roundingMode"));
        }

        record KeepFloat() implements DivisionRule {
        }

        record CastInt(RoundingMode roundingMode) implements DivisionRule {
        }

        record DecimalResult(int scale, RoundingMode roundingMode) implements DivisionRule {
        }
    }

    private final DecimalBackend decimalBackend;
    private final MixedTypePromotion mixedTypePromotion;
    private final DivisionRule divisionRule;
    private final DivisionByZeroPolicy divisionByZeroPolicy;

    private ArithmeticConfiguration(
            DecimalBackend decimalBackend,
            MixedTypePromotion mixedTypePromotion,
            DivisionRule divisionRule,
            DivisionByZeroPolicy divisionByZeroPolicy
    ) {
        this.decimalBackend = decimalBackend;
        this.mixedTypePromotion = mixedTypePromotion;
        this.divisionRule = divisionRule;
        this.divisionByZeroPolicy = divisionByZeroPolicy;
    }

    public static Builder init() {
        return new Builder();
    }

    public DecimalBackend decimalBackend() {
        return decimalBackend;
    }

    public MixedTypePromotion mixedTypePromotion() {
        return mixedTypePromotion;
    }

    public DivisionRule divisionRule() {
        return divisionRule;
    }

    public DivisionByZeroPolicy divisionByZeroPolicy() {
        return divisionByZeroPolicy;
    }

    public static final class Builder {
        private DecimalBackend decimalBackend = DecimalBackend.JAVA_BIG_DECIMAL;
        private MixedTypePromotion mixedTypePromotion = MixedTypePromotion.WIDEST;
        private DivisionRule divisionRule = DivisionRule.keepFloat();
        private DivisionByZeroPolicy divisionByZeroPolicy = DivisionByZeroPolicy.THROW;

        public Builder treatDecimalAsJavaBigDecimal() {
            this.decimalBackend = DecimalBackend.JAVA_BIG_DECIMAL;
            return this;
        }

        public Builder treatDecimalAsFloat() {
            this.decimalBackend = DecimalBackend.FLOAT;
            return this;
        }

        public Builder decimalBackend(DecimalBackend decimalBackend) {
            this.decimalBackend = Objects.requireNonNull(decimalBackend, "decimalBackend");
            return this;
        }

        public Builder mixedTypePromotion(MixedTypePromotion mixedTypePromotion) {
            this.mixedTypePromotion = Objects.requireNonNull(mixedTypePromotion, "mixedTypePromotion");
            return this;
        }

        public Builder onDivision(DivisionRule divisionRule) {
            this.divisionRule = Objects.requireNonNull(divisionRule, "divisionRule");
            return this;
        }

        public Builder roundOnDivision(RoundingMode mode) {
            Objects.requireNonNull(mode, "mode");
            this.divisionRule = switch (divisionRule) {
                case DivisionRule.CastInt _ -> DivisionRule.castInt(mode);
                case DivisionRule.DecimalResult(var scale, var _) -> DivisionRule.decimal(scale, mode);
                case DivisionRule.KeepFloat _ -> throw new IllegalStateException(
                        "roundOnDivision can only be used with castInt or decimal division"
                );
            };
            return this;
        }

        public Builder divisionScale(int scale) {
            if (scale < 0) {
                throw new IllegalArgumentException("division scale must be >= 0");
            }
            this.divisionRule = switch (divisionRule) {
                case DivisionRule.DecimalResult(var _, var mode) -> DivisionRule.decimal(scale, mode);
                case DivisionRule.CastInt _ -> throw new IllegalStateException(
                        "divisionScale can only be used with decimal division"
                );
                case DivisionRule.KeepFloat _ -> throw new IllegalStateException(
                        "divisionScale can only be used with decimal division"
                );
            };
            return this;
        }

        public Builder divisionByZero(DivisionByZeroPolicy divisionByZeroPolicy) {
            this.divisionByZeroPolicy = Objects.requireNonNull(divisionByZeroPolicy, "divisionByZeroPolicy");
            return this;
        }

        public ArithmeticPolicy build() {
            return buildConfiguration().toPolicy();
        }

        public ArithmeticConfiguration buildConfiguration() {
            validate();
            return new ArithmeticConfiguration(decimalBackend, mixedTypePromotion, divisionRule, divisionByZeroPolicy);
        }

        private void validate() {
            if (decimalBackend == DecimalBackend.FLOAT && divisionRule instanceof DivisionRule.DecimalResult) {
                throw new IllegalArgumentException(
                        "Decimal division result requires treatDecimalAsJavaBigDecimal()"
                );
            }
            if (divisionByZeroPolicy == DivisionByZeroPolicy.FLOAT_NAN
                    && !(divisionRule instanceof DivisionRule.KeepFloat)) {
                throw new IllegalArgumentException(
                        "FLOAT_NAN division by zero policy requires keepFloat division rule"
                );
            }
            if (divisionRule instanceof DivisionRule.DecimalResult(var scale, var _) && scale < 0) {
                throw new IllegalArgumentException("division scale must be >= 0");
            }
        }
    }

    ArithmeticPolicy toPolicy() {
        return new ConfigurableTypedArithmeticPolicy(this);
    }
}
