package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.run.ExecutionBudget;

import java.util.ArrayList;
import java.util.List;

public class MapLiteralRecorder implements TraceRecorder {

    private final ExecutionBudget budget;

    private final List<KeyValueRecorder> keyValueRecorderList = new ArrayList<>();

    MapLiteralRecorder(ExecutionBudget budget) {
        this.budget = budget;
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.MapLiteral(
                keyValueRecorderList.stream().map(
                        keyValueRecorder -> new TraceExpression.MapLiteral.MapEntry(
                                keyValueRecorder.keyRecorder.traceExpression(), keyValueRecorder.valueRecorder.traceExpression()
                        )).toList()
        );
    }

    public KeyValueRecorder nextKeyValueRecorder() {
        KeyValueRecorder keyValueRecorder = new KeyValueRecorder(
                new ExpressionRecorder(budget),
                new ExpressionRecorder(budget)
        );
        keyValueRecorderList.add(keyValueRecorder);
        return keyValueRecorder;
    }

    public record KeyValueRecorder(ExpressionRecorder keyRecorder, ExpressionRecorder valueRecorder) {
    }
}
