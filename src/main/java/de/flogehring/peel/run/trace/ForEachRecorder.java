package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForEachRecorder implements TraceRecorder {

    private String variableName;
    private ExpressionRecorder iterableExpressionRecorder;
    private final List<IterationRecorder> iterations = new ArrayList<>();

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.ForEachLoop(
                Objects.requireNonNull(variableName, "variableName"),
                Objects.requireNonNull(iterableExpressionRecorder, "iterableExpressionRecorder").traceExpression(),
                iterations.stream().map(
                        iteration -> new TraceExpression.ForEachLoop.Iteration(
                                new TraceExpression.ForEachLoop.VariableBinding(variableName, iteration.value()),
                                (TraceExpression.Block) iteration.bodyRecorder().traceExpression()
                        )
                ).toList()
        );
    }

    public void recordVariableName(String variableName) {
        this.variableName = variableName;
    }

    public ExpressionRecorder listRecorder() {
        iterableExpressionRecorder = new ExpressionRecorder();
        return iterableExpressionRecorder;
    }

    public ExpressionRecorder nextLoop(PeelValue currentValue) {
        ExpressionRecorder bodyRecorder = new ExpressionRecorder();
        iterations.add(new IterationRecorder(TraceValueMapper.fromPeelValue(currentValue), bodyRecorder));
        return bodyRecorder;
    }

    private record IterationRecorder(TraceValue value, ExpressionRecorder bodyRecorder) {
    }
}
