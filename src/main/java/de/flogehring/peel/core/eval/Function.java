package de.flogehring.peel.core.eval;


import de.flogehring.peel.core.values.PeelValue;

public interface Function {

    String name();

    int arity();

    PeelValue run(EvaluatedExpression... arguments);
}
