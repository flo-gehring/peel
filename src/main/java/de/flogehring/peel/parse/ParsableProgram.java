package de.flogehring.peel.parse;

import de.flogehring.jetpack.annotationmapper.FromChild;
import de.flogehring.jetpack.annotationmapper.FromRule;
import de.flogehring.jetpack.annotationmapper.creationstrategies.CreationStrategyConstructor;
import de.flogehring.jetpack.annotationmapper.creationstrategies.CreatorConstructor;
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