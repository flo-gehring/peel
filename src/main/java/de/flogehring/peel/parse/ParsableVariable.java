package de.flogehring.peel.parse;

import de.friendlyhedgehog.jetpack.annotationmapper.FromChild;
import de.friendlyhedgehog.jetpack.annotationmapper.FromRule;
import de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies.CreationStrategyConstructor;
import de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies.CreatorConstructor;
import de.flogehring.peel.core.lang.Expression;

@FromRule("Variable")
@CreationStrategyConstructor
public record ParsableVariable(@FromChild(index = 0) String name) implements ParsableExpression {

    @CreatorConstructor(order = {"name"})
    public ParsableVariable {

    }

    @Override
    public Expression toExpression() {
        return new Expression.VariableName(name);
    }
}
