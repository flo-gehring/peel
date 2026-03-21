package de.flogehring.peel.core.values;

import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.run.EvaluationEnvironment;
import lombok.Getter;

import java.util.List;

@Getter
public final class PeelClosure extends PeelCallable {

    private final EvaluationEnvironment evaluationEnvironment;

    public PeelClosure(String name, List<String> parameters, Expression.Block body, EvaluationEnvironment evaluationEnvironment) {
        super(name, parameters, body);
        this.evaluationEnvironment = evaluationEnvironment;
    }
}
