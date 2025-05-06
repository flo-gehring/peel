package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.lang.Program;

public interface Runtime {

    void register(Variable v);

    void register(Function f);

    EvaluatedProgram run(Program program);
}
