package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FunctionCallRecorder implements TraceRecorder {

    private final List<ExpressionRecorder> argumentRecorder = new ArrayList<>();
    private BlockTraceRecorder blockTraceRecorder;
    private ExpressionRecorder functionResolverRecorder;

    @Override
    public TraceExpression traceExpression() { // TODO
        return new TraceExpression.FunctionCall(
                "func",
                TraceValue.none(),
                List.of(),
                Optional.empty()
        );
    }

    public ExpressionRecorder recordArgument() {
        argumentRecorder.add(new ExpressionRecorder());
        return argumentRecorder.getLast();
    }

    public BlockTraceRecorder functionBody() {
        blockTraceRecorder = new BlockTraceRecorder();
        return blockTraceRecorder;
    }

    public void functionFromVar(Expression.VariableName variableName) {
    }

    public ExpressionRecorder functionFromExpr() {
        functionResolverRecorder = new ExpressionRecorder();
        return functionResolverRecorder;
    }
}
