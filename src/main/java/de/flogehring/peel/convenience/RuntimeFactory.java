package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.PeelRuntime;

public final class RuntimeFactory {

    private RuntimeFactory() {
    }

    public static PeelRuntime defaultLanguage() {
        return RuntimeBuilder.standardLanguage().build();
    }

    /**
     * Describes the same names and arities registered by {@link #defaultLanguage()}.
     *
     * @return immutable symbol descriptions for static analysis of the standard language
     */
    public static java.util.List<de.flogehring.peel.analysis.ProvidedSymbol> defaultSymbols() {
        return RuntimeBuilder.standardLanguage().symbols();
    }
}
