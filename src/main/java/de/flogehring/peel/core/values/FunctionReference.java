package de.flogehring.peel.core.values;

import de.flogehring.peel.core.eval.Function;
import lombok.Getter;

import java.util.List;

@Getter
public final class FunctionReference implements PeelCallable {

    private final String name;
    private final List<Function> functions;

    private FunctionReference(String name, List<Function> functions) {
        this.name = name;
        this.functions = functions;
    }

    public static FunctionReference of(String name, List<Function> functions) {
        return new FunctionReference(name, functions);
    }
}
