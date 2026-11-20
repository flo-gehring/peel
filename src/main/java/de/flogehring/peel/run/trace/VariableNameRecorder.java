package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.ExecutionBudget;

public class VariableNameRecorder implements TraceRecorder {

    private final ExecutionBudget budget;
    private String name;
    private TraceValue traceValue;

    VariableNameRecorder(ExecutionBudget budget) {
        this.budget = budget;
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.VariableName(name, traceValue);
    }

    public void recordVarName(String name) {
        this.name = name;
    }

    public void recordValue(PeelValue value) {
        this.traceValue = TraceValueMapper.fromPeelValue(value, budget);
    }
}
