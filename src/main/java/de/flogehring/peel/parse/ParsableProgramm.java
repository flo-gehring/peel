package de.flogehring.peel.parse;

import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.run.exceptions.PeelException;

import java.util.List;

sealed interface ParsableProgramm {

    Expression toExpr();

    record Programm(List<ParsableProgramm> programm) implements ParsableProgramm {
        @Override
        public Expression toExpr() {
            throw new PeelException("Parsing error");
        }

        Program toProgramm() {
            return new Program(
                    new Expression.Block(programm.stream().map(
                            this::toCodeElement
                    ).toList()
                    )
            );
        }

        private Expression toCodeElement(ParsableProgramm parsableProgramm) {
            if (parsableProgramm instanceof ParsableCodeElement(var expression)) {
                return expression;
            } else {
                throw new PeelException("parsing error");
            }
        }
    }

    record ParsableCodeElement(Expression codeElement) implements ParsableProgramm {
        @Override
        public Expression toExpr() {
            if (codeElement instanceof Expression expression) {
                return expression;
            } else {
                throw new PeelException("Parse error");
            }
        }
    }
}
