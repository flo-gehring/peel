package de.flogehring.peel.parse;

import de.flogehring.jetpack.annotationmapper.Delegate;
import de.flogehring.jetpack.annotationmapper.FromRule;
import de.flogehring.peel.core.lang.CodeElement;

@FromRule("CodeElement")
@Delegate(clazz = ParsableExpression.class, transitive = true)
@Delegate(clazz = ParsableStatement.class, transitive = true)
public interface ParsableCodeElement {

    CodeElement toCodeElement();
}
