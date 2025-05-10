package de.flogehring.peel.parse;

import de.flogehring.jetpack.annotationmapper.FromChild;
import de.flogehring.jetpack.annotationmapper.FromRule;
import de.flogehring.jetpack.annotationmapper.creationstrategies.CreationStrategyReflection;
import de.flogehring.peel.core.TypeDescriptor;
import de.flogehring.peel.core.lang.Expression;

@FromRule("Literal")
@CreationStrategyReflection
public class ParsableLiteralNumber implements ParsableExpression {

    @FromChild(index = 0)
    public Integer number;

    @Override
    public Expression toExpression() {
        return new Expression.Literal(TypeDescriptor.type(Number.class), number);
    }
}
