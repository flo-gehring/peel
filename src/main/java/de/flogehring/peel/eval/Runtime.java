package de.flogehring.peel.eval;

import de.flogehring.peel.lang.Program;

public interface Runtime {

    void register(Variable v);

    void register(Function f);

    Object run(Program program);
}
