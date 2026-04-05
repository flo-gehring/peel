package de.flogehring.peel.core.eval;

import java.util.List;

public record RuntimeModule(
        List<Variable> variables,
        List<Function> functions,
        List<OperatorDef> operators
) {
    public RuntimeModule {
        variables = List.copyOf(variables);
        functions = List.copyOf(functions);
        operators = List.copyOf(operators);
    }

    public static RuntimeModule empty() {
        return new RuntimeModule(List.of(), List.of(), List.of());
    }

    public static RuntimeModule of(
            List<Variable> variables,
            List<Function> functions,
            List<OperatorDef> operators
    ) {
        return new RuntimeModule(variables, functions, operators);
    }
}
