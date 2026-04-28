package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;

public class SelectorRecorder implements TraceRecorder {

    private ExpressionRecorder targetRecorder;
    private ExpressionRecorder selectorRecorder;
    private TraceValue peelValue;

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.Selector(
                peelValue,
                targetRecorder.traceExpression(),
                selectorRecorder.traceExpression()
        );
    }

    public ExpressionRecorder targetRecorder() {
        targetRecorder = new ExpressionRecorder();
        return targetRecorder;
    }

    public ExpressionRecorder selectorRecorder() {
        selectorRecorder = new ExpressionRecorder();
        return selectorRecorder;
    }

    public void recordValue(PeelValue peelValue) {
        this.peelValue = TraceValueMapper.fromPeelValue(peelValue);
    }
}
