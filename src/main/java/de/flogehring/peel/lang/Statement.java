package de.flogehring.peel.lang;

public sealed interface Statement extends CodeElement {

    record Assignment(String variableName, Expression assignment) implements Statement {

    }
}
