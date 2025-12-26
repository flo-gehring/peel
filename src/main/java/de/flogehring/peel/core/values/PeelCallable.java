package de.flogehring.peel.core.values;

import de.flogehring.peel.core.lang.Expression;
import lombok.Getter;

import java.util.List;

@Getter
public final class PeelCallable implements PeelValue {

    private final String name;
    private final List<String> parameters;
    private final Expression.Block body;

    private PeelCallable(String name, List<String> parameters, Expression.Block body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    public static PeelCallable userDefinedFunction(
            String name,
            List<String> parameters,
            Expression.Block body
    ) {
        return new PeelCallable(
                name,
                parameters,
                body
        );
    }
}
