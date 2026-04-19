package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.PeelFunctionDefinition;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.trace.BlockTraceRecorder;
import de.flogehring.peel.run.trace.FunctionCallRecorder;

import java.util.List;

public class PeelCodeFunction implements Function {

    private final PeelFunctionDefinition callable;
    private final EvaluationEnvironment environment;

    public PeelCodeFunction(PeelFunctionDefinition callable, EvaluationEnvironment environment) {
        this.callable = callable;
        this.environment = environment;
    }

    @Override
    public String name() {
        return callable.getName();
    }

    @Override
    public int arity() {
        return callable.getParameters().size();
    }

    @Override
    public List<String> argNames() {
        return callable.getParameters();
    }

    @Override
    public PeelValue run(PeelValue... arguments) {
        EvaluationEnvironment e = environment.spawnChild();
        e.enterScope();
        for (int i = 0; i < callable.getParameters().size(); ++i) {
            e.put(new Expression.VariableName(callable.getParameters().get(i), 0), arguments[i]);
        }
        return new Evaluator(e).evaluateBlock(callable.getBody().codeElements(), new BlockTraceRecorder());
    }

    @Override
    public PeelValue runWithTrace(FunctionCallRecorder functionCallRecorder, PeelValue... arguments) {
        functionCallRecorder.setName(callable.getName());
        EvaluationEnvironment e = environment.spawnChild();
        e.enterScope();
        for (int i = 0; i < callable.getParameters().size(); ++i) {
            String parameterName = callable.getParameters().get(i);
            functionCallRecorder.recordBinding(parameterName);
            e.put(new Expression.VariableName(parameterName, 0), arguments[i]);
        }
        return new Evaluator(e).evaluateBlock(callable.getBody().codeElements(), functionCallRecorder.functionBody());
    }
}