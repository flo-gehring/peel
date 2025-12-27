package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.EvaluatedExpression;
import lombok.Getter;

public class ReturnValueFlow extends RuntimeException {
    @Getter
    private final EvaluatedExpression expr;

    public ReturnValueFlow(EvaluatedExpression expression) {
        super("");
        expr = expression;
    }
}
