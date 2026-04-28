package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;
import lombok.Setter;

public class BinaryOpRecorder implements TraceRecorder {

    @Setter
    private String operator;
    private ExpressionRecorder lhsRecorder;
    private ExpressionRecorder rhsRecorder;
    private boolean shortCircuit = false;
    private TraceValue traceValue;

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.BinaryOperator(
                operator,
                traceValue,
                lhsRecorder.traceExpression(),
                shortCircuit ? null : rhsRecorder.traceExpression(),
                shortCircuit
        );
    }

    public ExpressionRecorder recordLhs() {
        lhsRecorder = new ExpressionRecorder();
        return lhsRecorder;
    }

    public void setShortCircuit() {
        shortCircuit = true;
    }

    public ExpressionRecorder recordRhs() {
        rhsRecorder = new ExpressionRecorder();
        return rhsRecorder;
    }

    public void recordValue(PeelValue value) {
        traceValue = TraceValueMapper.fromPeelValue(value);
    }
}
