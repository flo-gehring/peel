package de.flogehring.peel.eval;

public sealed interface EvaluatedStatement extends EvaluatedCodeElement {

    record Assignment(String variableName, EvaluatedExpression expression) implements EvaluatedStatement {
    }
}
