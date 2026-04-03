package de.flogehring.peel.core.values;

import de.flogehring.peel.core.lang.Expression;

import java.util.List;


public sealed interface PeelCallable extends PeelValue permits PeelFunctionDefinition, FunctionReference {

    static PeelFunctionDefinition userDefinedFunction(String name, List<String> parameters, Expression.Block body) {
        return new PeelFunctionDefinition(name, parameters, body);
    }
}
