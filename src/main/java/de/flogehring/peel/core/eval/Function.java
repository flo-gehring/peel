package de.flogehring.peel.core.eval;


import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.trace.SingleTraceExpressionRecorder;

import java.util.List;

public interface Function {

    String name();

    int arity();

    PeelValue run(PeelValue... arguments);

    default PeelValue runWithTrace(SingleTraceExpressionRecorder functionCallRecorder, PeelValue... arguments) {
        functionCallRecorder.append(new TraceExpression.Block(List.of()));
        return run(arguments);
    }
}
