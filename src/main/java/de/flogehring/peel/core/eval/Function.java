package de.flogehring.peel.core.eval;


import de.flogehring.peel.core.trace.CallableKind;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.trace.FunctionCallRecorder;

public interface Function {

    String name();

    int arity();

    default CallableKind callableKind() {
        return CallableKind.HOST_FUNCTION;
    }

    PeelValue runWithTrace(FunctionCallRecorder functionCallRecorder, PeelValue... arguments);
}
