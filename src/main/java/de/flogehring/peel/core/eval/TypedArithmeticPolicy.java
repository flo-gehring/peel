package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;

public abstract class TypedArithmeticPolicy implements ArithmeticPolicy {

    protected enum BinaryOp {
        ADD,
        SUB,
        MUL,
        DIV
    }

    protected enum UnaryOp {
        NEGATE
    }

    @Override
    public final PeelValue add(Number lhs, Number rhs) {
        return dispatch(BinaryOp.ADD, lhs, rhs);
    }

    @Override
    public final PeelValue sub(Number lhs, Number rhs) {
        return dispatch(BinaryOp.SUB, lhs, rhs);
    }

    @Override
    public final PeelValue mul(Number lhs, Number rhs) {
        return dispatch(BinaryOp.MUL, lhs, rhs);
    }

    @Override
    public final PeelValue div(Number lhs, Number rhs) {
        return dispatch(BinaryOp.DIV, lhs, rhs);
    }

    @Override
    public final PeelValue negate(Number value) {
        return dispatch(UnaryOp.NEGATE, value);
    }

    private PeelValue dispatch(BinaryOp op, Number lhs, Number rhs) {
        return switch (lhs) {
            case Number.Integer _ -> switch (rhs) {
                case Number.Integer _ -> on(op, (Number.Integer) lhs, (Number.Integer) rhs);
                case Number.Float _ -> on(op, (Number.Integer) lhs, (Number.Float) rhs);
                case Number.Decimal _ -> on(op, (Number.Integer) lhs, (Number.Decimal) rhs);
            };
            case Number.Float _ -> switch (rhs) {
                case Number.Integer _ -> on(op, (Number.Float) lhs, (Number.Integer) rhs);
                case Number.Float _ -> on(op, (Number.Float) lhs, (Number.Float) rhs);
                case Number.Decimal _ -> on(op, (Number.Float) lhs, (Number.Decimal) rhs);
            };
            case Number.Decimal _ -> switch (rhs) {
                case Number.Integer _ -> on(op, (Number.Decimal) lhs, (Number.Integer) rhs);
                case Number.Float _ -> on(op, (Number.Decimal) lhs, (Number.Float) rhs);
                case Number.Decimal _ -> on(op, (Number.Decimal) lhs, (Number.Decimal) rhs);
            };
        };
    }

    private PeelValue dispatch(UnaryOp op, Number value) {
        return switch (value) {
            case Number.Integer _ -> on(op, (Number.Integer) value);
            case Number.Float _ -> on(op, (Number.Float) value);
            case Number.Decimal _ -> on(op, (Number.Decimal) value);
        };
    }

    protected abstract PeelValue on(BinaryOp op, Number.Integer lhs, Number.Integer rhs);

    protected abstract PeelValue on(BinaryOp op, Number.Integer lhs, Number.Float rhs);

    protected abstract PeelValue on(BinaryOp op, Number.Integer lhs, Number.Decimal rhs);

    protected abstract PeelValue on(BinaryOp op, Number.Float lhs, Number.Integer rhs);

    protected abstract PeelValue on(BinaryOp op, Number.Float lhs, Number.Float rhs);

    protected abstract PeelValue on(BinaryOp op, Number.Float lhs, Number.Decimal rhs);

    protected abstract PeelValue on(BinaryOp op, Number.Decimal lhs, Number.Integer rhs);

    protected abstract PeelValue on(BinaryOp op, Number.Decimal lhs, Number.Float rhs);

    protected abstract PeelValue on(BinaryOp op, Number.Decimal lhs, Number.Decimal rhs);

    protected abstract PeelValue on(UnaryOp op, Number.Integer value);

    protected abstract PeelValue on(UnaryOp op, Number.Float value);

    protected abstract PeelValue on(UnaryOp op, Number.Decimal value);
}
