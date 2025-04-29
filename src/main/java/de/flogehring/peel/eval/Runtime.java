package de.flogehring.peel.eval;

import de.flogehring.peel.lang.Program;

public interface Runtime {

    void register(Variable v);

    void register(Function f);

    // TODO:
    // Should the result be an EvaluatedExpression or something with more info? Like a "VariableStore"
    EvaluatedExpression run(Program program);
}
