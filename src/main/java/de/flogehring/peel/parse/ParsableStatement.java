package de.flogehring.peel.parse;

import de.flogehring.jetpack.annotationmapper.Delegate;
import de.flogehring.jetpack.annotationmapper.FromRule;

@FromRule("Statement")
@Delegate(clazz = ParsableAssignment.class, transitive = true)
public interface ParsableStatement extends ParsableCodeElement {

}
