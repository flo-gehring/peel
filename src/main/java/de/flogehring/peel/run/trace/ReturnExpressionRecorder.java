package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import lombok.Getter;

public class ReturnExpressionRecorder implements TraceRecorder {

    @Getter
    private final ExpressionRecorder expressionRecorder;

    public ReturnExpressionRecorder() {
        expressionRecorder = new ExpressionRecorder();
    }

    @Override
    public TraceExpression traceExpression() {
        TraceExpression expression = expressionRecorder.traceExpression();
        return new TraceExpression.ReturnExpr(expression.value(), expression);
    }
}
