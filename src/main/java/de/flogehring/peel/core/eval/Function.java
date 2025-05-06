package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.TypeDescriptor;

import java.util.List;

public interface Function {

    String name();

    List<TypeDescriptor> arguments();

    EvaluatedExpression run(EvaluatedExpression... arguments);
}
