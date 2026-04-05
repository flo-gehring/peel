package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.eval.RuntimeModule;
import de.flogehring.peel.core.eval.Variable;
import de.flogehring.peel.core.lang.ExpressionFactoryMethods;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Bool;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.core.values.Text;
import de.flogehring.peel.run.exceptions.NumericOperatorOverrideException;
import de.flogehring.peel.run.exceptions.ReservedOperatorOverrideException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RuntimeBuilderTest {

    @Test
    void moduleContributionsAreApplied() {
        RuntimeModule module = RuntimeModule.of(
                List.of(Variable.of("x", PeelValue.text("a")), Variable.of("y", PeelValue.text("b"))),
                List.of(),
                List.of(OperatorDef.typed(
                        "~",
                        Text.class,
                        Text.class,
                        (lhs, rhs) -> PeelValue.text(((Text) lhs).value() + ((Text) rhs).value())
                ))
        );

        PeelRuntime runtime = RuntimeBuilder.create()
                .withModule(module)
                .build();

        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "~", ExpressionFactoryMethods.var("y"))
        ));

        assertThat(runtime.run(p).getLastExpression().value()).isEqualTo(PeelValue.text("ab"));
    }

    @Test
    void numericOverridesAreBlockedByDefault() {
        assertThatExceptionOfType(NumericOperatorOverrideException.class).isThrownBy(
                () -> RuntimeBuilder.standardLanguage()
                        .withOperator(OperatorDef.typed(
                                "~",
                                Number.class,
                                Number.class,
                                (lhs, rhs) -> PeelValue.integer(0)
                        ))
                        .build()
        );
    }

    @Test
    void numericOverridesCanBeEnabledExplicitly() {
        PeelRuntime runtime = RuntimeBuilder.standardLanguage()
                .allowNumericOperatorOverrides()
                .withOperator(OperatorDef.typed(
                        "~",
                        Number.class,
                        Number.class,
                        (lhs, rhs) -> PeelValue.integer(999)
                ))
                .withVariable(Variable.of("x", PeelValue.integer(1)))
                .withVariable(Variable.of("y", PeelValue.integer(2)))
                .build();

        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "~", ExpressionFactoryMethods.var("y"))
        ));

        assertThat(runtime.run(p).getLastExpression().value()).isEqualTo(PeelValue.integer(999));
    }

    @Test
    void overridingShortCircuitAndIsBlocked() {
        assertThatExceptionOfType(ReservedOperatorOverrideException.class).isThrownBy(
                () -> RuntimeBuilder.standardLanguage()
                        .withOperator(OperatorDef.typed(
                                "&&",
                                Bool.class,
                                Bool.class,
                                (lhs, rhs) -> PeelValue.bool(true)
                        ))
                        .build()
        );
    }

    @Test
    void overridingShortCircuitOrIsBlocked() {
        assertThatExceptionOfType(ReservedOperatorOverrideException.class).isThrownBy(
                () -> RuntimeBuilder.standardLanguage()
                        .withOperator(OperatorDef.typed(
                                "||",
                                Bool.class,
                                Bool.class,
                                (lhs, rhs) -> PeelValue.bool(true)
                        ))
                        .build()
        );
    }

    @Test
    void moduleCannotContributeReservedShortCircuitOperator() {
        RuntimeModule module = RuntimeModule.of(
                List.of(),
                List.of(),
                List.of(OperatorDef.typed(
                        "&&",
                        Bool.class,
                        Bool.class,
                        (lhs, rhs) -> PeelValue.bool(true)
                ))
        );

        assertThatExceptionOfType(ReservedOperatorOverrideException.class).isThrownBy(
                () -> RuntimeBuilder.standardLanguage()
                        .withModule(module)
                        .build()
        );
    }

    @Test
    void numericOverrideFlagDoesNotAllowReservedShortCircuitOverrides() {
        assertThatExceptionOfType(ReservedOperatorOverrideException.class).isThrownBy(
                () -> RuntimeBuilder.standardLanguage()
                        .allowNumericOperatorOverrides()
                        .withOperator(OperatorDef.typed(
                                "&&",
                                Bool.class,
                                Bool.class,
                                (lhs, rhs) -> PeelValue.bool(true)
                        ))
                        .build()
        );
    }
}
