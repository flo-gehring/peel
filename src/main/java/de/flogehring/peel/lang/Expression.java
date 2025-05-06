package de.flogehring.peel.lang;

import de.flogehring.peel.eval.TypeDescriptor;

import java.util.List;

public sealed interface Expression extends CodeElement {

    // TODO Now the Type Descriptor is part of the language, this is weird. Can this be avoided?
    record Literal(TypeDescriptor typeDescriptor, Object literal) implements Expression {
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