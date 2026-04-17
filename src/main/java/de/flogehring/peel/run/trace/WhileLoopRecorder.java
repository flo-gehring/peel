package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;

import java.util.ArrayList;
import java.util.List;

public class WhileLoopRecorder implements TraceRecorder {

    private List<ExpressionRecorder> conditions = new ArrayList<>();
    private List<ExpressionRecorder> bodies = new ArrayList<>();

    public ExpressionRecorder nextCondition() {
        conditions.add(new ExpressionRecorder());
        return conditions.getLast();
    }

    public ExpressionRecorder nextBody() {
        bodies.add(new ExpressionRecorder());
        return bodies.getLast();
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.WhileLoop(
                List.of(), // TODO
                new TraceValue.NoneValue() // TODO
        );
    }
}
