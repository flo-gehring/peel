package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.run.ExecutionBudget;

import java.util.ArrayList;
import java.util.List;

public class ListLiteralRecorder implements TraceRecorder {

    private final ExecutionBudget budget;

    private final List<ExpressionRecorder> listLiterals = new ArrayList<>();

    ListLiteralRecorder(ExecutionBudget budget) {
        this.budget = budget;
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.ListLiteral(
                listLiterals.stream().map(TraceRecorder::traceExpression).toList(),
                listLiterals.isEmpty() ? TraceValue.none() : listLiterals.getLast().traceExpression().value()
        );
    }

    public ExpressionRecorder subElementRecorder() {
        listLiterals.add(new ExpressionRecorder(budget));
        return listLiterals.getLast();
    }
}
