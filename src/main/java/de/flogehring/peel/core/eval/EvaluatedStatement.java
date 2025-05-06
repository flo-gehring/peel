package de.flogehring.peel.core.eval;

public sealed interface EvaluatedStatement extends EvaluatedCodeElement {

    record Assignment(String variableName, EvaluatedExpression expression) implements EvaluatedStatement {
    }
}
