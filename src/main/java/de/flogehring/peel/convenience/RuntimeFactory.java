package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.PeelRuntime;

public final class RuntimeFactory {

    private RuntimeFactory() {
    }

    public static PeelRuntime defaultLanguage() {
        return RuntimeBuilder.standardLanguage().build();
    }
}
