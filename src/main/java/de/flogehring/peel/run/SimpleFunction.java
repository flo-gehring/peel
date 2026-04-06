package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.trace.SingleTraceExpressionRecorder;

import java.util.List;

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
    public PeelValue run(PeelValue... arguments) {
        return function.apply(arguments);
    }

    @Override
    public PeelValue runWithTrace(SingleTraceExpressionRecorder functionCallRecorder, PeelValue... arguments) {
        // TODO Function call seems wonky here
        functionCallRecorder.append(new TraceExpression.Block(List.of()));
        return run(arguments);
    }
}
