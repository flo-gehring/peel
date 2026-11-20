package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.run.ExecutionBudget;
import lombok.Getter;

public class ReturnExpressionRecorder implements TraceRecorder {

    @Getter
    private final ExpressionRecorder expressionRecorder;

    ReturnExpressionRecorder(ExecutionBudget budget) {
        expressionRecorder = new ExpressionRecorder(budget);
    }

    @Override
    public TraceExpression traceExpression() {
        TraceExpression expression = expressionRecorder.traceExpression();
        return new TraceExpression.ReturnExpr(expression.value(), expression);
    }
}
