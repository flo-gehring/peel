package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

import java.util.ArrayList;
import java.util.List;

public class IfElseRecorder implements TraceRecorder {

    private final List<ExpressionRecorder> conditions = new ArrayList<>();
    private ExpressionRecorder blockRecorder;

    public ExpressionRecorder nextCondition() {
        conditions.add(new ExpressionRecorder());
        return conditions.getLast();
    }

    public ExpressionRecorder recordBlock() {
        blockRecorder = new ExpressionRecorder();
        return blockRecorder;
    }

    public ExpressionRecorder recordElse() {
        blockRecorder = new ExpressionRecorder();
        return blockRecorder;
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.IfStatement(
                conditions.stream().map(TraceRecorder::traceExpression).toList(),
                blockRecorder != null ? blockRecorder.traceExpression() : null
        );
    }
}
