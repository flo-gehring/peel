package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.ExecutionBudget;

public class LiteralRecorder implements TraceRecorder {

    private final ExecutionBudget budget;

    TraceValue value;

    LiteralRecorder(ExecutionBudget budget) {
        this.budget = budget;
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.Literal(value);
    }

    public void recordLiteral(PeelValue enriched) {
        value = TraceValueMapper.fromPeelValue(enriched, budget);
    }
}
