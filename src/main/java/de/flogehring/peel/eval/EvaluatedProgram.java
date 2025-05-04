package de.flogehring.peel.eval;

import java.util.List;

public record EvaluatedProgram(List<EvaluatedCodeElement> evaluatedCodeElement) {

    public EvaluatedExpression getLastExpression() {
        return evaluatedCodeElement.stream()
                .filter(EvaluatedExpression.class::isInstance)
                .map(EvaluatedExpression.class::cast)
                .toList()
                .reversed()
                .getFirst();
    }
}
