package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.*;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.core.lang.Program;

import java.util.ArrayList;
import java.util.Optional;

public class SimpleRuntime implements Runtime {

    private final Scope global;

    public static SimpleRuntime empty() {
        return new SimpleRuntime(Scope.empty());
    }

    private SimpleRuntime(Scope global) {
        this.global = global;
    }

    @Override
    public void register(Variable v) {
        global.register(v);
    }

    @Override
    public void register(Function f) {
        global.register(f);
    }

    @Override
    public EvaluatedProgram run(Program program) {
        EvaluationEnvironment environment = new EvaluationEnvironment(
                global,
                Optional.empty(),
                new ArrayList<>()
        );
        Evaluator evaluator = new Evaluator(environment);
        EvaluatedExpression evaluated = evaluator.evaluate(program.programm());
        EvaluatedExpression.EvaluatedBlock block = (EvaluatedExpression.EvaluatedBlock) evaluated;
        return new EvaluatedProgram(block.content());
    }
}