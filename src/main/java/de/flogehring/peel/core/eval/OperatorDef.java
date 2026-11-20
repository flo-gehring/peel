package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.ExecutionBudget;
import lombok.Getter;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class OperatorDef {

    private final String symbol;
    private final int arity;
    private final Class<? extends PeelValue> lhsType;
    private final Class<? extends PeelValue> rhsType;
    private final Function<PeelValue, PeelValue> unaryImplementation;
    private final BudgetedBinaryOperator implementation;

    @FunctionalInterface
    public interface BudgetedBinaryOperator {
        PeelValue apply(ExecutionBudget budget, PeelValue lhs, PeelValue rhs);
    }
    @Getter
    private final boolean arithmeticManaged;

    private OperatorDef(
            String symbol,
            int arity,
            Class<? extends PeelValue> lhsType,
            Class<? extends PeelValue> rhsType,
            Function<PeelValue, PeelValue> unaryImplementation,
            BudgetedBinaryOperator implementation,
            boolean arithmeticManaged
    ) {
        this.symbol = Objects.requireNonNull(symbol, "symbol");
        this.arity = arity;
        this.lhsType = Objects.requireNonNull(lhsType, "lhsType");
        this.rhsType = Objects.requireNonNull(rhsType, "rhsType");
        this.unaryImplementation = Objects.requireNonNull(unaryImplementation, "unaryImplementation");
        this.implementation = Objects.requireNonNull(implementation, "implementation");
        this.arithmeticManaged = arithmeticManaged;
    }

    public static OperatorDef typed(
            String symbol,
            Class<? extends PeelValue> argType,
            Function<PeelValue, PeelValue> implementation
    ) {
        return new OperatorDef(
                symbol,
                1,
                argType,
                PeelValue.class,
                implementation,
                (_, _, _) -> {
                    throw new IllegalStateException("Unary operator can't be called as binary");
                },
                false
        );
    }

    public static OperatorDef typed(
            String symbol,
            Class<? extends PeelValue> lhsType,
            Class<? extends PeelValue> rhsType,
            BiFunction<PeelValue, PeelValue, PeelValue> implementation
    ) {
        Objects.requireNonNull(implementation, "implementation");
        return typedWithBudget(symbol, lhsType, rhsType, (_, lhs, rhs) -> implementation.apply(lhs, rhs));
    }

    public static OperatorDef typedWithBudget(
            String symbol,
            Class<? extends PeelValue> lhsType,
            Class<? extends PeelValue> rhsType,
            BudgetedBinaryOperator implementation
    ) {
        return new OperatorDef(
                symbol,
                2,
                lhsType,
                rhsType,
                _ -> {
                    throw new IllegalStateException("Binary operator can't be called as unary");
                },
                implementation,
                false
        );
    }

    public static OperatorDef arithmeticManaged(
            String symbol,
            Class<? extends PeelValue> argType,
            Function<PeelValue, PeelValue> implementation
    ) {
        return new OperatorDef(
                symbol,
                1,
                argType,
                PeelValue.class,
                implementation,
                (_, _, _) -> {
                    throw new IllegalStateException("Unary operator can't be called as binary");
                },
                true
        );
    }

    public static OperatorDef arithmeticManaged(
            String symbol,
            Class<? extends PeelValue> lhsType,
            Class<? extends PeelValue> rhsType,
            BiFunction<PeelValue, PeelValue, PeelValue> implementation
    ) {
        Objects.requireNonNull(implementation, "implementation");
        return arithmeticManagedWithBudget(symbol, lhsType, rhsType, (budget, lhs, rhs) -> implementation.apply(lhs, rhs));
    }

    public static OperatorDef arithmeticManagedWithBudget(
            String symbol,
            Class<? extends PeelValue> lhsType,
            Class<? extends PeelValue> rhsType,
            BudgetedBinaryOperator implementation
    ) {
        return new OperatorDef(
                symbol,
                2,
                lhsType,
                rhsType,
                _ -> {
                    throw new IllegalStateException("Binary operator can't be called as unary");
                },
                implementation,
                true
        );
    }

    public String symbol() {
        return symbol;
    }

    public Class<? extends PeelValue> lhsType() {
        return lhsType;
    }

    public Class<? extends PeelValue> rhsType() {
        return rhsType;
    }

    public String signature() {
        if (arity == 1) {
            return "(" + lhsType.getSimpleName() + ")";
        }
        return "(" + lhsType.getSimpleName() + ", " + rhsType.getSimpleName() + ")";
    }

    public boolean matches(PeelValue argument) {
        return arity == 1 && lhsType.isInstance(argument);
    }

    public boolean matches(PeelValue lhs, PeelValue rhs) {
        return arity == 2 && lhsType.isInstance(lhs) && rhsType.isInstance(rhs);
    }

    public PeelValue apply(PeelValue argument) {
        return unaryImplementation.apply(argument);
    }

    public PeelValue apply(PeelValue lhs, PeelValue rhs, ExecutionBudget budget) {
        return implementation.apply(budget, lhs, rhs);
    }

    public boolean acceptsNumericPair() {
        return arity == 2 && Number.class.isAssignableFrom(lhsType) && Number.class.isAssignableFrom(rhsType);
    }
}
