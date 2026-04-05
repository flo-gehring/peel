package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.*;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.run.SimpleRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RuntimeBuilder {

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

    public Runtime build() {
        ArithmeticPolicy resolvedPolicy = arithmeticPolicy == null ? ArithmeticPolicies.standard() : arithmeticPolicy;
        SimpleRuntime runtime = SimpleRuntime.empty(allowNumericOperatorOverrides);
        ArithmeticPolicies.registerOperators(runtime, resolvedPolicy);
        modules.forEach(module -> {
            module.variables().forEach(runtime::register);
            module.functions().forEach(runtime::register);
            module.operators().forEach(runtime::register);
        });
        variables.forEach(runtime::register);
        functions.forEach(runtime::register);
        operators.forEach(runtime::register);
        return runtime;
    }
}
