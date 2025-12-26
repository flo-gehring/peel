package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.values.PeelValue;

public class SimpleFunction implements Function {

    private final String name;
    private final int arity;
    private final java.util.function.Function<EvaluatedExpression[], PeelValue> function;

    public SimpleFunction(
            String name,
            int arity,
            java.util.function.Function<EvaluatedExpression[], PeelValue> function
    ) {
        this.name = name;
        this.arity = arity;
        this.function = function;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int arity() {
        return arity;
    }

    @Override
    public PeelValue run(EvaluatedExpression... arguments) {
        return function.apply(arguments);
    }
}