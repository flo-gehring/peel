package de.flogehring.peel.parse;

import de.friendlyhedgehog.jetpack.annotationmapper.FromChild;
import de.friendlyhedgehog.jetpack.annotationmapper.FromRule;
import de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies.CreationStrategyConstructor;
import de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies.CreatorConstructor;
import de.flogehring.peel.core.lang.Expression;

@FromRule("BinaryOperator")
@CreationStrategyConstructor
public record ParsableBinaryOperator(
        @FromChild(index = 0)
        ParsableExpression lhs,
        @FromChild(index = 1)
        String operator,
        @FromChild(index = 2)
        ParsableExpression rhs
) implements ParsableExpression {

    @CreatorConstructor(order = {"lhs", "operator", "rhs"})
    public ParsableBinaryOperator {

    }

    @Override
    public Expression toExpression() {
        return new Expression.BinaryOperator(
                operator,
                lhs.toExpression(),
                rhs.toExpression()
        );
    }
}
