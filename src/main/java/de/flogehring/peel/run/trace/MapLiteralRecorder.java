package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

import java.util.ArrayList;
import java.util.List;

public class MapLiteralRecorder implements TraceRecorder {

    private final List<KeyValueRecorder> keyValueRecorderList = new ArrayList<>();

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
                new ExpressionRecorder(),
                new ExpressionRecorder()
        );
        keyValueRecorderList.add(keyValueRecorder);
        return keyValueRecorder;
    }

    public record KeyValueRecorder(ExpressionRecorder keyRecorder, ExpressionRecorder valueRecorder) {
    }
}
