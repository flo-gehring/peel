package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.PeelFunctionDefinition;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.trace.SingleTraceExpressionRecorder;
import de.flogehring.peel.run.trace.TraceSubRecorder;

import java.util.ArrayList;
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
    public PeelValue run(PeelValue... arguments) {
        EvaluationEnvironment e = environment.spawnChild();
        e.enterScope();
        for (int i = 0; i < callable.getParameters().size(); ++i) {
            e.put(new Expression.VariableName(callable.getParameters().get(i), 0), arguments[i]);
        }
        TraceSubRecorder bodyRecorder = new TraceSubRecorder();
        return new Evaluator(e).evaluateBlock(callable.getBody().codeElements(), bodyRecorder);
    }

    @Override
    public PeelValue runWithTrace(SingleTraceExpressionRecorder functionCallRecorder, PeelValue... arguments) {
        EvaluationEnvironment e = environment.spawnChild();
        e.enterScope();
        // TODO add parameter bindings
        List<TraceExpression.ParameterBinding> parameterBindings = new ArrayList<>();
        for (int i = 0; i < callable.getParameters().size(); ++i) {
            String parameterName = callable.getParameters().get(i);
            e.put(new Expression.VariableName(parameterName, 0), arguments[i]);
            parameterBindings.add(new TraceExpression.ParameterBinding(
                    parameterName,
                    new TraceExpression.Literal(TraceValueMapper.fromPeelValue(arguments[i]))
            ));
        }
        TraceSubRecorder traceSubRecorder = new TraceSubRecorder();
        functionCallRecorder.append(traceSubRecorder);
        return new Evaluator(e).evaluateBlock(callable.getBody().codeElements(), traceSubRecorder);
    }
}