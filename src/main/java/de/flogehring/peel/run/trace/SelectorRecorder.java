package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.ExecutionBudget;

public class SelectorRecorder implements TraceRecorder {

    private final ExecutionBudget budget;

    private ExpressionRecorder targetRecorder;
    private ExpressionRecorder selectorRecorder;
    private TraceValue peelValue;

    SelectorRecorder(ExecutionBudget budget) {
        this.budget = budget;
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.Selector(
                peelValue,
                targetRecorder.traceExpression(),
                selectorRecorder.traceExpression()
        );
    }

    public ExpressionRecorder targetRecorder() {
        targetRecorder = new ExpressionRecorder(budget);
        return targetRecorder;
    }

    public ExpressionRecorder selectorRecorder() {
        selectorRecorder = new ExpressionRecorder(budget);
        return selectorRecorder;
    }

    public void recordValue(PeelValue peelValue) {
        this.peelValue = TraceValueMapper.fromPeelValue(peelValue, budget);
    }
}
