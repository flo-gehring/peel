package de.flogehring.peel.parse;

import de.flogehring.jetpack.annotationmapper.FromChild;
import de.flogehring.jetpack.annotationmapper.FromRule;
import de.flogehring.jetpack.annotationmapper.creationstrategies.CreationStrategyConstructor;
import de.flogehring.jetpack.annotationmapper.creationstrategies.CreatorConstructor;
import de.flogehring.peel.core.lang.CodeElement;
import de.flogehring.peel.core.lang.Statement;

@FromRule("Assignment")
@CreationStrategyConstructor
public record ParsableAssignment(
        @FromChild(index = 0) String variableName,
        @FromChild(index = 2) ParsableExpression parsableExpression
) implements ParsableCodeElement {

    @CreatorConstructor(order = {"variableName", "parsableExpression"})
    public ParsableAssignment {

    }

    @Override
    public CodeElement toCodeElement() {
        return new Statement.Assignment(
                variableName, parsableExpression.toExpression()
        );
    }
}