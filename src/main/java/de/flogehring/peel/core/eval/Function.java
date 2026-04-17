package de.flogehring.peel.core.eval;


import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.trace.FunctionCallRecorder;

import java.util.List;

public interface Function {

    String name();

    int arity();

    List<String> argNames();

    PeelValue run(PeelValue... arguments);

    PeelValue runWithTrace(FunctionCallRecorder functionCallRecorder, PeelValue... arguments);
}
