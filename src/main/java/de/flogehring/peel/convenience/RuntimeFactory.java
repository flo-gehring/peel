package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.types.Number;
import de.flogehring.peel.core.types.Text;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.SimpleRuntime;

import static de.flogehring.peel.run.SimpleRuntime.empty;

public class RuntimeFactory {

    private RuntimeFactory() {

    }

    /**
     * Register useful Operators and Functions to the Runtime.
     *
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
                new Text(),
                new Text(),
                (lhs, rhs) -> new PeelValue.Primitive(new Text(), lhs.value() + ((String) rhs.value()
                )));
    }

    private static Function countSubstring() {
        return FunctionFactory.binary(
                "count",
                new Text(), new Text(),
                (lhs, rhs) -> {
                    String running = (String) lhs.value();
                    String rhsS = (String) rhs.value();
                    int occurences = 0;
                    while ((running).contains(rhsS)) {
                        occurences++;
                        running = running.replaceFirst(rhsS, "");
                    }
                    return new PeelValue.Primitive(new de.flogehring.peel.core.types.Number.Integer(), occurences);
                }
        );
    }

    private static Function addNumbers() {
        return FunctionFactory.binary(
                "+",
                new Number.Integer(), new Number.Integer(),
                (lhs, rhs) -> new PeelValue.Primitive(
                        new Number.Integer(),
                        ((Integer) lhs.value()) + ((Integer) rhs.value())
                )
        );
    }
}
