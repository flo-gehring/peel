package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.ExecutionBudget;

public interface ArithmeticPolicy {

    PeelValue add(Number lhs, Number rhs);

    PeelValue sub(Number lhs, Number rhs);

    PeelValue mul(Number lhs, Number rhs);

    PeelValue mod(Number lhs, Number rhs);

    PeelValue pow(Number lhs, Number rhs);

    default PeelValue pow(Number lhs, Number rhs, ExecutionBudget budget) {
        return pow(lhs, rhs);
    }

    PeelValue div(Number lhs, Number rhs);

    PeelValue negate(Number value);
}
