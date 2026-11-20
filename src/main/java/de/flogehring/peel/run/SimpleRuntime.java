package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.ExecutionInterrupt;
import de.flogehring.peel.core.eval.ExecutionLimits;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.eval.RequestBindings;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.DuplicateRequestBindingException;
import de.flogehring.peel.run.trace.ExpressionRecorder;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class SimpleRuntime implements PeelRuntime {

    private final Scope global;

    private SimpleRuntime(Scope global) {
        this.global = global;
    }

    public static SimpleRuntime fromGlobalScope(Scope global) {
        return new SimpleRuntime(global.copy());
    }

    @Override
    public TraceProgram run(Program program) {
        return run(program, RequestBindings.empty());
    }

    /**
     * Evaluates with a budget enforced by this runtime and its built-ins.
     */
    public TraceProgram run(Program program, RequestBindings requestBindings, ExecutionLimits limits) {
        return run(program, requestBindings, new ExecutionBudget(limits));
    }

    @Override
    public TraceProgram run(Program program, RequestBindings requestBindings) {
        return run(program, requestBindings, ExecutionBudget.unlimited());
    }

    private TraceProgram run(Program program, RequestBindings requestBindings, ExecutionBudget budget) {
        budget.checkpoint();
        budget.requireValueSize(requestBindings.values().size());
        Scope runGlobal = global.copy();
        applyRequestBindings(runGlobal, requestBindings, budget);
        EvaluationEnvironment environment = new EvaluationEnvironment(
                runGlobal,
                Optional.empty(),
                new ArrayList<>()
        );
        Evaluator evaluator = new Evaluator(environment, budget);
        ExpressionRecorder expressionRecorder = new ExpressionRecorder(budget);
        PeelValue result = evaluator.evaluate(program.programm(), expressionRecorder);
        budget.checkpoint();
        var block = (TraceExpression.Block) expressionRecorder.traceExpression();
        var trace = new TraceProgram(
                block.content(),
                TraceValueMapper.fromPeelValue(result, budget),
                block.variablesAtExit()
        );
        budget.checkpoint();
        return trace;
    }

    private void applyRequestBindings(
            Scope runGlobal, RequestBindings requestBindings, ExecutionBudget budget
    ) {
        for (Map.Entry<String, PeelValue> entry : requestBindings.values().entrySet()) {
            ExecutionInterrupt.check();
            budget.checkBinding(entry.getValue());
            String name = entry.getKey();
            if (runGlobal.hasVar(name)) {
                throw new DuplicateRequestBindingException(name);
            }
            runGlobal.putVar(name, entry.getValue());
        }
    }
}
