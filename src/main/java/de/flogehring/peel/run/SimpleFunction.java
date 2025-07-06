package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.types.PeelTypes;
import de.flogehring.peel.core.values.PeelValue;

import java.util.Arrays;
import java.util.List;

public class SimpleFunction implements Function {

    private final String name;
    private final List<PeelTypes> arguments;
    private final java.util.function.Function<EvaluatedExpression[], PeelValue> function;

    public SimpleFunction(
            String name,
            List<PeelTypes> arguments,
            java.util.function.Function<EvaluatedExpression[], PeelValue> function
    ) {
        this.name = name;
        this.arguments = arguments;
        this.function = function;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<PeelTypes> arguments() {
        return arguments;
    }

    @Override
    public EvaluatedExpression run(EvaluatedExpression... arguments) {
        return new EvaluatedExpression.FunctionCall(
                name,
                function.apply(arguments),
                Arrays.stream(arguments).toList()
        );
    }
}
