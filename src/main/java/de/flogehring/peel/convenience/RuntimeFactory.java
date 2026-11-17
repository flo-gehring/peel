package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.PeelRuntime;

public final class RuntimeFactory {

    private RuntimeFactory() {
    }

    public static PeelRuntime defaultLanguage() {
        return RuntimeBuilder.standardLanguage().build();
    }

    public static java.util.List<de.flogehring.peel.analysis.ProvidedSymbol> defaultSymbols() {
        return RuntimeBuilder.standardLanguage().symbols();
    }
}
