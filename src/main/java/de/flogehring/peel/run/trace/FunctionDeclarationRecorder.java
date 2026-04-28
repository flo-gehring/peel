package de.flogehring.peel.run.trace;

import de.flogehring.peel.core.trace.CallableKind;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValue;

import java.util.List;

public class FunctionDeclarationRecorder implements TraceRecorder {

    private String name;
    private List<String> parameters;

    @Override
    public TraceExpression traceExpression() {
        return new TraceExpression.Literal(new TraceValue.CallableRef(
                CallableKind.PEEL_FUNCTION, name, parameters
        ));
    }

    public void recordCallable(String name, List<String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }
}
