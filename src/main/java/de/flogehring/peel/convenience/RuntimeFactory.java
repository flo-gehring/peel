package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.run.SimpleRuntime;

import static de.flogehring.peel.run.SimpleRuntime.empty;

public class RuntimeFactory {

    private RuntimeFactory() {

    }

    /**
     * Register useful Operators and Functions to the Runtime.
     * @return Prepopulated Runtime
     */
    public static SimpleRuntime defaultLanguage() {
        SimpleRuntime runtime = empty();
        runtime.register(addNumbers());
        runtime.register(addStrings());
        runtime.register(countSubstring());
        return runtime;
    }

    private static Function addStrings() {
        return FunctionFactory.binary(
                "+",
                String.class, String.class, String.class, (lhs, rhs) -> lhs + rhs
        );
    }

    private static Function countSubstring() {
        return FunctionFactory.binary(
                "count",
                String.class, String.class, Number.class, (lhs, rhs) -> {
                    int occurences = 0;
                    while (lhs.contains(rhs)) {
                        occurences++;
                        lhs = lhs.replaceFirst(rhs, "");
                    }
                    return occurences;
                }
        );
    }

    private static Function addNumbers() {
        return FunctionFactory.binary(
                "+",
                Number.class, Number.class, Number.class, (lhs, rhs) -> lhs.doubleValue() + rhs.doubleValue()
        );
    }
}
