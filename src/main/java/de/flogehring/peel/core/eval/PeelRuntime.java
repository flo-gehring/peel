package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.lang.Program;

public interface PeelRuntime {

    EvaluatedProgram run(Program program);

    EvaluatedProgram run(Program program, RequestBindings requestBindings);
}
