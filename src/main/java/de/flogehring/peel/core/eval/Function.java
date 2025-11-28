package de.flogehring.peel.core.eval;


public interface Function {

    String name();

    int arity();

    EvaluatedExpression run(EvaluatedExpression... arguments);
}
