package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.run.ExecutionBudget;

import java.util.ArrayList;
import java.util.List;

public class IfElseRecorder implements TraceRecorder {

    private final ExecutionBudget budget;

    private final List<ExpressionRecorder> conditions = new ArrayList<>();
    private ExpressionRecorder blockRecorder;

    IfElseRecorder(ExecutionBudget budget) {
        this.budget = budget;
    }

    public ExpressionRecorder nextCondition() {
        conditions.add(new ExpressionRecorder(budget));
        return conditions.getLast();
    }

    public ExpressionRecorder recordBlock() {
        blockRecorder = new ExpressionRecorder(budget);
        return blockRecorder;
    }

    public ExpressionRecorder recordElse() {
        blockRecorder = new ExpressionRecorder(budget);
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
