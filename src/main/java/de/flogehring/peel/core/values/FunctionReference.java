package de.flogehring.peel.core.values;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;

import java.util.List;
import java.util.stream.IntStream;

public final class FunctionReference extends PeelCallable {

    public static FunctionReference of(Function f) {
        return new FunctionReference(
                f.name(),
                IntStream.range(0, f.arity()).mapToObj(i -> "arg" + i).toList(),
                new Expression.Block(List.of())
        );
    }

    private FunctionReference(String name, List<String> parameters, Expression.Block body) {
        super(name, parameters, body);
    }
}
