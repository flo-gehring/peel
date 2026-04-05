package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.*;
import de.flogehring.peel.run.Scope;
import de.flogehring.peel.run.SimpleRuntime;
import de.flogehring.peel.run.exceptions.NumericOperatorOverrideException;
import de.flogehring.peel.run.exceptions.ReservedOperatorOverrideException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RuntimeBuilder {

    private static final Set<String> RESERVED_LANGUAGE_OPERATORS = Set.of("&&", "||");

    private final List<RuntimeModule> modules = new ArrayList<>();
    private final List<Function> functions = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();
    private final List<OperatorDef> operators = new ArrayList<>();
    private ArithmeticPolicy arithmeticPolicy;
    private boolean allowNumericOperatorOverrides;

    private RuntimeBuilder() {
    }

    public static RuntimeBuilder create() {
        return new RuntimeBuilder();
    }

    public static RuntimeBuilder standard() {
        return create().withArithmetic(ArithmeticPolicies.standard());
    }

    public static RuntimeBuilder standardLanguage() {
        return standard().withModule(StandardRuntimeModule.INSTANCE);
    }

    public RuntimeBuilder withArithmetic(ArithmeticPolicy arithmeticPolicy) {
        this.arithmeticPolicy = Objects.requireNonNull(arithmeticPolicy, "arithmeticPolicy");
        return this;
    }

    public RuntimeBuilder allowNumericOperatorOverrides() {
        this.allowNumericOperatorOverrides = true;
        return this;
    }

    public RuntimeBuilder withModule(RuntimeModule module) {
        this.modules.add(Objects.requireNonNull(module, "module"));
        return this;
    }

    public RuntimeBuilder withFunction(Function function) {
        this.functions.add(Objects.requireNonNull(function, "function"));
        return this;
    }

    public RuntimeBuilder withVariable(Variable variable) {
        this.variables.add(Objects.requireNonNull(variable, "variable"));
        return this;
    }

    public RuntimeBuilder withOperator(OperatorDef operator) {
        this.operators.add(Objects.requireNonNull(operator, "operator"));
        return this;
    }

    public PeelRuntime build() {
        ArithmeticPolicy resolvedPolicy = arithmeticPolicy == null ? ArithmeticPolicies.standard() : arithmeticPolicy;
        List<Variable> allVariables = new ArrayList<>();
        List<Function> allFunctions = new ArrayList<>();
        List<OperatorDef> allOperators = new ArrayList<>();

        modules.forEach(module -> {
            allVariables.addAll(module.variables());
            allFunctions.addAll(module.functions());
            allOperators.addAll(module.operators());
        });

        allVariables.addAll(variables);
        allFunctions.addAll(functions);

        allOperators.addAll(ArithmeticPolicies.operatorDefinitions(resolvedPolicy));
        allOperators.addAll(operators);
        validateOperatorOverrides(allOperators);

        Scope globalScope = Scope.from(allVariables, allFunctions, allOperators);
        return SimpleRuntime.fromGlobalScope(globalScope);
    }

    private void validateOperatorOverrides(List<OperatorDef> operatorDefs) {
        operatorDefs.stream()
                .filter(def -> RESERVED_LANGUAGE_OPERATORS.contains(def.symbol()))
                .findFirst()
                .ifPresent(def -> {
                    throw new ReservedOperatorOverrideException(def.symbol());
                });
        if (allowNumericOperatorOverrides) {
            return;
        }
        operatorDefs.stream()
                .filter(def -> def.acceptsNumericPair() && !def.isArithmeticManaged())
                .findFirst()
                .ifPresent(def -> {
                    throw new NumericOperatorOverrideException(def.symbol());
                });
    }
}
