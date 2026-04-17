package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

import java.util.ArrayList;
import java.util.List;

public class ForEachRecorder implements TraceRecorder {

    private ExpressionRecorder listValueRecorder;
    private final List<ExpressionRecorder> loopBodyRecorder = new ArrayList<>();

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.ForEachLoop(
                loopBodyRecorder.stream().map(
                        body -> new TraceExpression.ForEachLoop.Iteration(
                                (TraceExpression.Block) body.traceExpression()
                        )
                ).toList()
        );
    }

    public ExpressionRecorder listRecorder() {
        listValueRecorder = new ExpressionRecorder();
        return listValueRecorder;
    }

    public ExpressionRecorder nextLoop() {
        ExpressionRecorder bodyRecorder = new ExpressionRecorder();
        loopBodyRecorder.add(bodyRecorder);
        return bodyRecorder;
    }
}
