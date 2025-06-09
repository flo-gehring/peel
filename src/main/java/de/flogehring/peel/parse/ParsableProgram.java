package de.flogehring.peel.parse;

import de.friendlyhedgehog.jetpack.annotationmapper.FromChild;
import de.friendlyhedgehog.jetpack.annotationmapper.FromRule;
import de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies.CreationStrategyConstructor;
import de.friendlyhedgehog.jetpack.annotationmapper.creationstrategies.CreatorConstructor;
import de.flogehring.peel.core.lang.Program;

import java.util.List;

@FromRule("Program")
@CreationStrategyConstructor
public class ParsableProgram {

    @FromChild(index = 0)
    private final List<ParsableCodeElement> parsableCodeElements;

    @CreatorConstructor(
            order = {"parsableCodeElements"}
    )
    public ParsableProgram(List<ParsableCodeElement> elements) {
        parsableCodeElements = elements;
    }

    public Program toProgramm() {
        return new Program(
                parsableCodeElements.stream()
                        .map(ParsableCodeElement::toCodeElement)
                        .toList()
        );
    }
}