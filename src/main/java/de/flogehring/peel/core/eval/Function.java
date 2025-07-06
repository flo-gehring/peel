package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.types.PeelTypes;

import java.util.List;

public interface Function {

    String name();

    List<PeelTypes> arguments();

    EvaluatedExpression run(EvaluatedExpression... arguments);
}
