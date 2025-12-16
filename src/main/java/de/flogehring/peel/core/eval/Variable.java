package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.PeelValue;

public interface Variable {

    String name();

    PeelValue value();
}
