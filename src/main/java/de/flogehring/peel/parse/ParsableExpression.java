package de.flogehring.peel.parse;

import de.friendlyhedgehog.jetpack.annotationmapper.Delegate;
import de.friendlyhedgehog.jetpack.annotationmapper.FromRule;
import de.flogehring.peel.core.lang.CodeElement;
import de.flogehring.peel.core.lang.Expression;

@FromRule("Expression")
@Delegate(clazz = ParsableLiteralNumber.class, transitive = true)
@Delegate(clazz = ParsableBinaryOperator.class, transitive = true)
@Delegate(clazz = ParsableVariable.class, transitive = true)
public interface ParsableExpression extends ParsableCodeElement {

    Expression toExpression();

    @Override
    default CodeElement toCodeElement() {
        return toExpression();
    }
}
