package de.flogehring.peel.core.lang;

import java.util.List;

public record Program(Expression.Block programm) {

    public Program(List<Expression> expressionList) {
        this(new Expression.Block(expressionList));
    }
}
