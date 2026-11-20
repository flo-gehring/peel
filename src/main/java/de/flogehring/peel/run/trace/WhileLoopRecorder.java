package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.run.ExecutionBudget;

import java.util.ArrayList;
import java.util.List;

public class WhileLoopRecorder implements TraceRecorder {

    private final ExecutionBudget budget;

    private final List<ExpressionRecorder> conditions = new ArrayList<>();
    private final List<ExpressionRecorder> bodies = new ArrayList<>();

    WhileLoopRecorder(ExecutionBudget budget) {
        this.budget = budget;
    }

    public ExpressionRecorder nextCondition() {
        conditions.add(new ExpressionRecorder(budget));
        return conditions.getLast();
    }

    public ExpressionRecorder nextBody() {
        bodies.add(new ExpressionRecorder(budget));
        return bodies.getLast();
    }

    @Override
    public TraceExpression traceExpression() {
        List<TraceExpression.WhileLoop.Iteration> iteration = new ArrayList<>();
        TraceValue lastValue = TraceValue.NONE_VALUE;
        for (int i = 0; i < conditions.size(); ++i) {
            TraceExpression.Block body = null;
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
