package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.SimpleFunction;

import java.util.function.BinaryOperator;

public class FunctionFactory {

    private FunctionFactory() {
    }

    public static Function binary(
            String name,
            BinaryOperator<PeelValue> function
    ) {
        return new SimpleFunction(
                name,
                2,
                arguments -> function.apply(
                        arguments[0].value(),
                        arguments[1].value()

                )
        );
    }
}