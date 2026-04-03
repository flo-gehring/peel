package de.flogehring.peel.core.values;

import de.flogehring.peel.core.eval.Function;
import lombok.Getter;

import java.util.List;

@Getter
public final class FunctionReference implements PeelCallable {

    private final List<Function> functions;

    private FunctionReference(List<Function> functions) {
        this.functions = functions;
    }

    public static FunctionReference of(List<Function> functions) {
        return new FunctionReference(functions);
    }
}
