package de.flogehring.peel.core.values;

import de.flogehring.peel.core.lang.Expression;
import lombok.Getter;

import java.util.List;

@Getter
public sealed class PeelFunctionDefinition implements PeelCallable permits PeelClosure {

    private final String name;
    private final List<String> parameters;
    private final Expression.Block body;

    protected PeelFunctionDefinition(String name, List<String> parameters, Expression.Block body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }
}
