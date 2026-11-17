package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockTraceRecorder implements TraceRecorder {

    private final List<ExpressionRecorder> traceContents = new ArrayList<>();
    private String name;
    private Map<String, TraceValue> variablesAtExit = Map.of();

    public void recordVariables(Map<String, TraceValue> values) {
        variablesAtExit = Map.copyOf(values);
    }

    public void recordName(String name) {
        this.name = name;
    }

    public ExpressionRecorder nextRecorder() {
        ExpressionRecorder expressionRecorder = new ExpressionRecorder();
        traceContents.add(expressionRecorder);
        return expressionRecorder;
    }

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.Block(
                traceContents.stream().map(TraceRecorder::traceExpression).toList(), name, variablesAtExit
        );
    }
}
