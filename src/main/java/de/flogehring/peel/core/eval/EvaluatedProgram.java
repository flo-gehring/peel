package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.None;

import java.util.List;

public record EvaluatedProgram(List<EvaluatedExpression> evaluatedCodeElement) {

    public EvaluatedExpression getLastExpression() {
        if (evaluatedCodeElement.isEmpty()) {
            return new EvaluatedExpression.Literal(None.NONE);
        }
        return evaluatedCodeElement.getLast();
    }
}
