package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;

import java.util.ArrayList;
import java.util.List;

public class WhileLoopRecorder implements TraceRecorder {

    public static final TraceExpression.Block EMPTY_BLOCK = new TraceExpression.Block(List.of());
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
        List<TraceExpression.WhileLoop.Iteration> iteration = new ArrayList<>();
        TraceValue lastValue = TraceValue.NONE_VALUE;
        for (int i = 0; i < conditions.size(); ++i) {
            TraceExpression.Block body = EMPTY_BLOCK;
            if (bodies.size() > i) {
                body = (TraceExpression.Block) bodies.get(i).traceExpression();
                lastValue = body.value();
            }
            iteration.add(
                    new TraceExpression.WhileLoop.Iteration(
                            conditions.get(i).traceExpression(),
                            body
                    )
            );
        }
        return new TraceExpression.WhileLoop(iteration, lastValue);
    }
}
