package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.PeelValue;

public interface Variable {

    String name();

    PeelValue value();

    static Variable of(String name, PeelValue value) {
        return new Variable() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public PeelValue value() {
                return value;
            }
        };
    }
}
