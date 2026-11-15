package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

import java.util.ArrayList;
import java.util.List;

public class BlockTraceRecorder implements TraceRecorder {

    private final List<ExpressionRecorder> traceContents = new ArrayList<>();
    private String name;

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
                traceContents.stream().map(TraceRecorder::traceExpression).toList(), name
        );
    }
}
