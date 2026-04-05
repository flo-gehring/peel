package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.eval.RequestBindings;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.DuplicateRequestBindingException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class SimpleRuntime implements PeelRuntime {

    private final Scope global;

    private SimpleRuntime(Scope global) {
        this.global = global;
    }

    public static PeelRuntime fromGlobalScope(Scope global) {
        return new SimpleRuntime(global.copy());
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
