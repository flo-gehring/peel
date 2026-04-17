package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.TraceExpression;

public class AssignmentRecorder implements TraceRecorder {

    private ExpressionRecorder assignmentRecorder;
    private String varName;

    @Override
    public TraceExpression traceExpression() {
        TraceExpression expression = assignmentRecorder.traceExpression();
        return new TraceExpression.Assignment(
                varName, expression, expression.value()

        );
    }

    public ExpressionRecorder recordAssignmentExpression() {
        assignmentRecorder = new ExpressionRecorder();
        return assignmentRecorder;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }
}
