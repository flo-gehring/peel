package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.core.values.Text;
import de.flogehring.peel.run.exceptions.AmbiguousOperatorException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class OperatorResolverTest {

    private final OperatorResolver resolver = new OperatorResolver();

    @Test
    void unaryNoCandidatesThrows() {
        assertThatExceptionOfType(NoFunctionFoundException.class).isThrownBy(
                () -> resolver.resolveAndApply("!", PeelValue.bool(true), List.of())
        );
    }

    @Test
    void unarySingleCandidateApplied() {
        PeelValue value = resolver.resolveAndApply(
                "!",
                PeelValue.bool(true),
                List.of(OperatorDef.typed("!", de.flogehring.peel.core.values.Bool.class, arg -> PeelValue.bool(false)))
        );
        assertThat(value).isEqualTo(PeelValue.bool(false));
    }

    @Test
    void binaryNoCandidatesThrows() {
        assertThatExceptionOfType(NoFunctionFoundException.class).isThrownBy(
                () -> resolver.resolveAndApply("+", PeelValue.integer(1), PeelValue.integer(2), List.of())
        );
    }

    @Test
    void binarySingleCandidateApplied() {
        PeelValue value = resolver.resolveAndApply(
                "~",
                PeelValue.text("a"),
                PeelValue.text("b"),
                List.of(OperatorDef.typed("~", Text.class, Text.class, (lhs, rhs) -> PeelValue.text("ok")))
        );
        assertThat(value).isEqualTo(PeelValue.text("ok"));
    }

    @Test
    void mostSpecificBinaryTypeWins() {
        PeelValue value = resolver.resolveAndApply(
                "~",
                new Number.Integer(1),
                new Number.Integer(2),
                List.of(
                        OperatorDef.typed("~", Number.class, Number.class, (lhs, rhs) -> PeelValue.text("generic")),
                        OperatorDef.typed("~", Number.Integer.class, Number.Integer.class, (lhs, rhs) -> PeelValue.text("specific"))
                )
        );
        assertThat(value).isEqualTo(PeelValue.text("specific"));
    }

    @Test
    void ambiguousBinaryTypeMatchThrows() {
        assertThatExceptionOfType(AmbiguousOperatorException.class).isThrownBy(
                () -> resolver.resolveAndApply(
                        "~",
                        new Number.Integer(1),
                        new Number.Integer(2),
                        List.of(
                                OperatorDef.typed("~", Number.class, Number.Integer.class, (lhs, rhs) -> PeelValue.text("a")),
                                OperatorDef.typed("~", Number.Integer.class, Number.class, (lhs, rhs) -> PeelValue.text("b"))
                        )
                )
        );
    }

    @Test
    void unaryMostSpecificTypeWins() {
        PeelValue value = resolver.resolveAndApply(
                "-",
                new Number.Integer(5),
                List.of(
                        OperatorDef.typed("-", Number.class, arg -> PeelValue.text("generic")),
                        OperatorDef.typed("-", Number.Integer.class, arg -> PeelValue.text("specific"))
                )
        );
        assertThat(value).isEqualTo(PeelValue.text("specific"));
    }

    @Test
    void unaryAmbiguousTypeMatchThrows() {
        assertThatExceptionOfType(AmbiguousOperatorException.class).isThrownBy(
                () -> resolver.resolveAndApply(
                        "u",
                        new Number.Integer(5),
                        List.of(
                                OperatorDef.typed("u", Number.class, arg -> PeelValue.text("a")),
                                OperatorDef.typed("u", Number.class, arg -> PeelValue.text("b"))
                        )
                )
        );
    }
}
