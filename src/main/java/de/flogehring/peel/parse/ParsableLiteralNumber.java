package de.flogehring.peel.parse;

import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.Number;
import de.friendlyhedgehog.jetpack.annotationmapper.FromChild;
import de.friendlyhedgehog.jetpack.annotationmapper.FromRule;
import de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies.CreationStrategyReflection;

@FromRule("Literal")
@CreationStrategyReflection
public class ParsableLiteralNumber implements ParsableExpression {

    @FromChild(index = 0)
    public Integer number;

    @Override
    public Expression toExpression() {
        return new Expression.Literal(new Number.Integer(number));
    }
}
