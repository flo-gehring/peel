package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;

import java.math.BigDecimal;
import java.math.MathContext;

public final class ConfigurableTypedArithmeticPolicy extends TypedArithmeticPolicy {

    private static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL128;

    private final ArithmeticConfiguration configuration;

    public ConfigurableTypedArithmeticPolicy(ArithmeticConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Integer lhs, Number.Integer rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Integer lhs, Number.Float rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Integer lhs, Number.Decimal rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Float lhs, Number.Integer rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Float lhs, Number.Float rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Float lhs, Number.Decimal rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Decimal lhs, Number.Integer rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Decimal lhs, Number.Float rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(BinaryOp op, Number.Decimal lhs, Number.Decimal rhs) {
        return binary(op, lhs, rhs);
    }

    @Override
    protected PeelValue on(UnaryOp op, Number.Integer value) {
        return unary(op, value);
    }

    @Override
    protected PeelValue on(UnaryOp op, Number.Float value) {
        return unary(op, value);
    }

    @Override
    protected PeelValue on(UnaryOp op, Number.Decimal value) {
        return unary(op, value);
    }

    private PeelValue unary(UnaryOp op, Number value) {
        return switch (op) {
            case NEGATE -> negateValue(value);
        };
    }

    private PeelValue negateValue(Number value) {
        return switch (value) {
            case Number.Integer(var integerValue) -> new Number.Integer(-integerValue);
            case Number.Float(var floatValue) -> new Number.Float(-floatValue);
            case Number.Decimal(var decimalValue) -> {
                if (configuration.decimalBackend() == ArithmeticConfiguration.DecimalBackend.FLOAT) {
                    yield new Number.Float(-decimalValue.floatValue());
                }
                yield new Number.Decimal(decimalValue.negate());
            }
        };
    }

    private PeelValue binary(BinaryOp op, Number lhs, Number rhs) {
        if (op == BinaryOp.DIV) {
            return divide(lhs, rhs);
        }
        NumericKind resultKind = resolveResultKind(lhs, rhs);
        return switch (resultKind) {
            case INTEGER -> integerResult(op, lhs, rhs);
            case FLOAT -> floatResult(op, lhs, rhs);
            case DECIMAL -> decimalResult(op, lhs, rhs);
        };
    }

    private PeelValue divide(Number lhs, Number rhs) {
        if (isZero(rhs)) {
            return switch (configuration.divisionByZeroPolicy()) {
                case THROW -> throw new ArithmeticException("Division by zero");
                case FLOAT_NAN -> new Number.Float(Float.NaN);
            };
        }
        return switch (configuration.divisionRule()) {
            case ArithmeticConfiguration.DivisionRule.KeepFloat ignored ->
                    new Number.Float(floatValue(lhs) / floatValue(rhs));
            case ArithmeticConfiguration.DivisionRule.CastInt(var roundingMode) -> {
                BigDecimal quotient = decimalValue(lhs).divide(decimalValue(rhs), DIVISION_CONTEXT);
                BigDecimal rounded = quotient.setScale(0, roundingMode);
                yield new Number.Integer(rounded.intValue());
            }
            case ArithmeticConfiguration.DivisionRule.DecimalResult(var scale, var roundingMode) -> {
                BigDecimal quotient = decimalValue(lhs).divide(decimalValue(rhs), scale, roundingMode);
                yield new Number.Decimal(quotient);
            }
        };
    }

    private boolean isZero(Number value) {
        return switch (value) {
            case Number.Integer(var integerValue) -> integerValue == 0;
            case Number.Float(var floatValue) -> floatValue == 0.0f;
            case Number.Decimal(var decimalValue) -> decimalValue.compareTo(BigDecimal.ZERO) == 0;
        };
    }

    private PeelValue integerResult(BinaryOp op, Number lhs, Number rhs) {
        int left = integerValue(lhs);
        int right = integerValue(rhs);
        return switch (op) {
            case ADD -> new Number.Integer(left + right);
            case SUB -> new Number.Integer(left - right);
            case MUL -> new Number.Integer(left * right);
            case DIV -> throw new IllegalStateException("Integer result for division is handled separately");
        };
    }

    private PeelValue floatResult(BinaryOp op, Number lhs, Number rhs) {
        float left = floatValue(lhs);
        float right = floatValue(rhs);
        return switch (op) {
            case ADD -> new Number.Float(left + right);
            case SUB -> new Number.Float(left - right);
            case MUL -> new Number.Float(left * right);
            case DIV -> throw new IllegalStateException("Float division is handled separately");
        };
    }

    private PeelValue decimalResult(BinaryOp op, Number lhs, Number rhs) {
        BigDecimal left = decimalValue(lhs);
        BigDecimal right = decimalValue(rhs);
        return switch (op) {
            case ADD -> new Number.Decimal(left.add(right));
            case SUB -> new Number.Decimal(left.subtract(right));
            case MUL -> new Number.Decimal(left.multiply(right));
            case DIV -> throw new IllegalStateException("Decimal division is handled separately");
        };
    }

    private NumericKind resolveResultKind(Number lhs, Number rhs) {
        NumericKind lhsKind = kindOf(lhs);
        NumericKind rhsKind = kindOf(rhs);
        return switch (configuration.mixedTypePromotion()) {
            case WIDEST -> widest(lhsKind, rhsKind);
            case DECIMAL_PREFERRED -> decimalPreferred(lhsKind, rhsKind);
            case FLOAT_PREFERRED -> floatPreferred(lhsKind, rhsKind);
        };
    }

    private NumericKind widest(NumericKind lhs, NumericKind rhs) {
        if (lhs == NumericKind.DECIMAL || rhs == NumericKind.DECIMAL) {
            return NumericKind.DECIMAL;
        }
        if (lhs == NumericKind.FLOAT || rhs == NumericKind.FLOAT) {
            return NumericKind.FLOAT;
        }
        return NumericKind.INTEGER;
    }

    private NumericKind decimalPreferred(NumericKind lhs, NumericKind rhs) {
        if (configuration.decimalBackend() == ArithmeticConfiguration.DecimalBackend.JAVA_BIG_DECIMAL
                && lhs != rhs) {
            return NumericKind.DECIMAL;
        }
        return widest(lhs, rhs);
    }

    private NumericKind floatPreferred(NumericKind lhs, NumericKind rhs) {
        if (lhs == NumericKind.FLOAT || rhs == NumericKind.FLOAT) {
            return NumericKind.FLOAT;
        }
        if (lhs == NumericKind.DECIMAL || rhs == NumericKind.DECIMAL) {
            return NumericKind.DECIMAL;
        }
        return NumericKind.INTEGER;
    }

    private NumericKind kindOf(Number value) {
        return switch (value) {
            case Number.Integer ignored -> NumericKind.INTEGER;
            case Number.Float ignored -> NumericKind.FLOAT;
            case Number.Decimal ignored ->
                    configuration.decimalBackend() == ArithmeticConfiguration.DecimalBackend.FLOAT
                            ? NumericKind.FLOAT
                            : NumericKind.DECIMAL;
        };
    }

    private int integerValue(Number number) {
        return switch (number) {
            case Number.Integer(var value) -> value;
            case Number.Float(var value) -> (int) value.floatValue();
            case Number.Decimal(var value) -> value.intValue();
        };
    }

    private float floatValue(Number number) {
        return switch (number) {
            case Number.Integer(var value) -> value;
            case Number.Float(var value) -> value;
            case Number.Decimal(var value) -> value.floatValue();
        };
    }

    private BigDecimal decimalValue(Number number) {
        return switch (number) {
            case Number.Integer(var value) -> BigDecimal.valueOf(value);
            case Number.Float(var value) -> BigDecimal.valueOf(value.doubleValue());
            case Number.Decimal(var value) -> {
                if (configuration.decimalBackend() == ArithmeticConfiguration.DecimalBackend.FLOAT) {
                    yield BigDecimal.valueOf(value.floatValue());
                }
                yield value;
            }
        };
    }

    private enum NumericKind {
        INTEGER,
        FLOAT,
        DECIMAL
    }
}
