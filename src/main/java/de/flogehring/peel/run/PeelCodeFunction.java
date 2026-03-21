package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.PeelCallable;
import de.flogehring.peel.core.values.PeelValue;

public class PeelCodeFunction implements Function {

    private final PeelCallable callable;
    private final EvaluationEnvironment environment;

    public PeelCodeFunction(PeelCallable callable, EvaluationEnvironment environment) {
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
    public PeelValue run(EvaluatedExpression... arguments) {
        de.flogehring.peel.run.EvaluationEnvironment e = environment.spawnChild();
        e.enterScope();
        for (int i = 0; i < callable.getParameters().size(); ++i) {
            e.put(
                    new Expression.VariableName(callable.getParameters().get(i), 0),
                    arguments[i].value()
            );
        }
        EvaluatedExpression.EvaluatedBlock evaluatedBlock = new Evaluator(e).evaluateBlock(callable.getBody().codeElements());
        e.exitScope();
        return evaluatedBlock.value();
    }
}
