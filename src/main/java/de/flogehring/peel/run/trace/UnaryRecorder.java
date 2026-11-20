package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.ExecutionBudget;

public class UnaryRecorder implements TraceRecorder {

    private final ExecutionBudget budget;
    private TraceValue traceValue;
    private ExpressionRecorder expressionRecorder;
    private String operator;

    UnaryRecorder(ExecutionBudget budget) {
        this.budget = budget;
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.UnaryPrefixOperator(
                operator,
                traceValue,
                expressionRecorder.traceExpression()
        );
    }

    public void recordOperator(String operator) {
        this.operator = operator;
    }

    public ExpressionRecorder recordOperand() {
        expressionRecorder = new ExpressionRecorder(budget);
        return expressionRecorder;
    }

    public void recordValue(PeelValue value) {
        this.traceValue = TraceValueMapper.fromPeelValue(value, budget);
    }
}
