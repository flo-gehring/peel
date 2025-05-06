package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.TypeDescriptor;

import java.util.Arrays;
import java.util.List;

public class SimpleFunction implements Function {

    private final String name;
    private final List<TypeDescriptor> arguments;
    private final TypeDescriptor returnType;
    private final java.util.function.Function<EvaluatedExpression[], Object> function;

    public SimpleFunction(
            String name,
            List<TypeDescriptor> arguments,
            TypeDescriptor returnType,
            java.util.function.Function<EvaluatedExpression[], Object> function
    ) {
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
        this.function = function;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<TypeDescriptor> arguments() {
        return arguments;
    }

    @Override
    public EvaluatedExpression run(EvaluatedExpression... arguments) {
        return new EvaluatedExpression.FunctionCall(
                name,
                returnType,
                function.apply(arguments),
                Arrays.stream(arguments).toList()
        );
    }
}
