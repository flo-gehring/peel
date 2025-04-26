package de.flogehring.peel;

public sealed interface Expression extends CodeElement {


    record Literal(Object literal) implements Expression {

    }

    record BinaryOperator(String operator, Expression lhs, Expression rhs) implements Expression {

    }

    record VariableName(String name) implements Expression {

    }

}
