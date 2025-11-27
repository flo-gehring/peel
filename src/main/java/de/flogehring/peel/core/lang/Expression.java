package de.flogehring.peel.core.lang;

import de.flogehring.peel.core.values.PeelValue;

import java.util.List;

public sealed interface Expression extends CodeElement {


    record Literal(PeelValue value) implements Expression {
    }

    record BinaryOperator(String operator, Expression lhs, Expression rhs) implements Expression {
    }

    record VariableName(String name) implements Expression {
    }

    record FunctionCall(
            String functionName,
            List<Expression> arguments
    ) implements Expression{

    }
}