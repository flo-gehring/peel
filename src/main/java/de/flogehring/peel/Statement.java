package de.flogehring.peel;

public sealed interface Statement extends CodeElement {

    record Assignment(String variableName, Expression assignment) implements Statement {

    }
}
