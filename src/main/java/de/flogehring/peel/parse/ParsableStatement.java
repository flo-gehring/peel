package de.flogehring.peel.parse;

import de.friendlyhedgehog.jetpack.annotationmapper.Delegate;
import de.friendlyhedgehog.jetpack.annotationmapper.FromRule;

@FromRule("Statement")
@Delegate(clazz = ParsableAssignment.class, transitive = true)
public interface ParsableStatement extends ParsableCodeElement {

}
