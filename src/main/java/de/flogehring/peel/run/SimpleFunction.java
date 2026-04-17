package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.trace.FunctionCallRecorder;

import java.util.List;
import java.util.stream.IntStream;

public class SimpleFunction implements Function {

    private final String name;
    private final int arity;
    private final java.util.function.Function<PeelValue[], PeelValue> function;

    public SimpleFunction(
            String name,
            int arity,
            java.util.function.Function<PeelValue[], PeelValue> function
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
    public List<String> argNames() {
        return IntStream.rangeClosed(0, arity)
                .mapToObj(String::valueOf)
                .map(n -> "arg" + n)
                .toList();
    }

    @Override
    public PeelValue run(PeelValue... arguments) {
        return function.apply(arguments);
    }

    @Override
    public PeelValue runWithTrace(FunctionCallRecorder functionCallRecorder, PeelValue... arguments) {
        // TODO Function call seems wonky here
        return run(arguments);
    }
}
