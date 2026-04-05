package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.Runtime;

public final class RuntimeFactory {

    private RuntimeFactory() {
    }

    public static Runtime defaultLanguage() {
        return RuntimeBuilder.standardLanguage().build();
    }
}
