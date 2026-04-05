package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;

public interface ArithmeticPolicy {

    PeelValue add(Number lhs, Number rhs);

    PeelValue sub(Number lhs, Number rhs);

    PeelValue mul(Number lhs, Number rhs);

    PeelValue mod(Number lhs, Number rhs);

    PeelValue pow(Number lhs, Number rhs);

    PeelValue div(Number lhs, Number rhs);

    PeelValue negate(Number value);
}
