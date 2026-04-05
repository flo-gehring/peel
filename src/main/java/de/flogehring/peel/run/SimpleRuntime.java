package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.*;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.DuplicateRequestBindingException;
import de.flogehring.peel.run.exceptions.NumericOperatorOverrideException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class SimpleRuntime implements Runtime {

    private final Scope global;
    private final boolean allowNumericOperatorOverrides;

    public static SimpleRuntime empty() {
        return new SimpleRuntime(Scope.empty(), false);
    }

    public static SimpleRuntime empty(boolean allowNumericOperatorOverrides) {
        return new SimpleRuntime(Scope.empty(), allowNumericOperatorOverrides);
    }

    private SimpleRuntime(Scope global, boolean allowNumericOperatorOverrides) {
        this.global = global;
        this.allowNumericOperatorOverrides = allowNumericOperatorOverrides;
    }

    public void register(Variable v) {
        global.register(v);
    }

    public void register(Function f) {
        global.register(f);
    }

    public void register(OperatorDef operatorDef) {
        if (operatorDef.acceptsNumericPair() && !allowNumericOperatorOverrides && !operatorDef.isArithmeticManaged()) {
            throw new NumericOperatorOverrideException(operatorDef.symbol());
        }
        global.register(operatorDef);
    }

    @Override
    public EvaluatedProgram run(Program program) {
        return run(program, RequestBindings.empty());
    }

    @Override
    public EvaluatedProgram run(Program program, RequestBindings requestBindings) {
        Scope runGlobal = global.copy();
        applyRequestBindings(runGlobal, requestBindings);
        EvaluationEnvironment environment = new EvaluationEnvironment(
                runGlobal,
                Optional.empty(),
                new ArrayList<>()
        );
        Evaluator evaluator = new Evaluator(environment);
        EvaluatedExpression evaluated = evaluator.evaluate(program.programm());
        EvaluatedExpression.EvaluatedBlock block = (EvaluatedExpression.EvaluatedBlock) evaluated;
        return new EvaluatedProgram(block.content());
    }

    private void applyRequestBindings(Scope runGlobal, RequestBindings requestBindings) {
        for (Map.Entry<String, PeelValue> entry : requestBindings.values().entrySet()) {
            String name = entry.getKey();
            if (runGlobal.hasVar(name)) {
                throw new DuplicateRequestBindingException(name);
            }
            runGlobal.putVar(name, entry.getValue());
        }
    }
}
