package de.flogehring.peel.core.values;

import java.math.BigDecimal;

public sealed interface Number extends Primitives {

    BigDecimal numberValue();

    record Integer(java.lang.Integer value) implements Number {
        @Override
        public BigDecimal numberValue() {
            return BigDecimal.valueOf(value.doubleValue());
        }
    }

    record Float(java.lang.Float value) implements Number {
        @Override
        public BigDecimal numberValue() {
            return BigDecimal.valueOf(value.doubleValue());
        }
    }

    record Decimal(BigDecimal value) implements Number {
        @Override
        public BigDecimal numberValue() {
            return value;
        }
    }
}
