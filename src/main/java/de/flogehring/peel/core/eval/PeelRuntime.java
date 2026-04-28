package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.trace.TraceProgram;

public interface PeelRuntime {

    TraceProgram run(Program program);

    TraceProgram run(Program program, RequestBindings requestBindings);
}
