package de.flogehring.peel.run;

import de.flogehring.peel.eval.EvaluatedExpression;
import de.flogehring.peel.eval.Function;
import de.flogehring.peel.eval.TypeDescriptor;
import de.flogehring.peel.eval.Variable;
import de.flogehring.peel.lang.CodeElement;
import de.flogehring.peel.lang.Program;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import static de.flogehring.peel.eval.TypeDescriptor.type;

public class SimpleRuntimeTest {

    @Test
    void simple() {
        Program p = new Program(List.of(
                CodeElement.assign("x", CodeElement.literal(1)),
                CodeElement.assign("y", CodeElement.literal(1)),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        Assertions.assertEquals(2.0, runtime.run(p).value());
    }

    @Test
    void stringAddition() {
        Program p = new Program(List.of(
                CodeElement.assign("x", CodeElement.literal("1")),
                CodeElement.assign("y", CodeElement.literal("1")),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        Assertions.assertEquals("11", runtime.run(p).value());
    }

    @Test
    void registerVariables() {
        Program p = new Program(List.of(
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        runtime.register(getVariable("x", "1"));
        runtime.register(getVariable("y", "2"));
        Assertions.assertEquals("12", runtime.run(p).value());
    }

    @Test
    void registerFunctions() {
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        runtime.register(new Function() {
            @Override
            public String name() {
                return "*";
            }

            @Override
            public List<TypeDescriptor> arguments() {
                return List.of(type(String.class), type(Integer.class));
            }

            @Override
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                EvaluatedExpression argumentLhs = arguments[0];
                EvaluatedExpression argumentRhs = arguments[1];
                String lhs = (String) argumentLhs.value();
                int rhs = (Integer) argumentRhs.value();
                return new EvaluatedExpression.BinaryOperator(
                        "*", type(String.class), repeatString(lhs, rhs), argumentLhs, argumentRhs
                );
            }

            private String repeatString(String lhs, int rhs) {
                return lhs.repeat(rhs);
            }
        });
        Program p = new Program(List.of(
                CodeElement.expr(CodeElement.var("x"), "*", CodeElement.var("y"))
        ));
        runtime.register(getVariable("x", "Echo!"));
        runtime.register(getVariable("y", 2));
        assertThat(runtime.run(p).value()).isEqualTo("Echo!Echo!");
    }

    @Test
    void multipleFunctionDefinitions() {
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        runtime.register(new Function() {
            @Override
            public String name() {
                return "+";
            }

            @Override
            public List<TypeDescriptor> arguments() {
                return List.of(type(Integer.class), type(Integer.class));
            }

            @Override
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                EvaluatedExpression argumentLhs = arguments[0];
                EvaluatedExpression argumentRhs = arguments[1];
                Integer lhs = (Integer) argumentLhs.value();
                Integer rhs = (Integer) argumentRhs.value();
                return new EvaluatedExpression.BinaryOperator(
                        "+", type(Integer.class), lhs + rhs, argumentLhs, argumentRhs
                );
            }
        });
        runtime.register(getVariable("y", 2));
        runtime.register(getVariable("x", 1));
        Program p = new Program(List.of(
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        assertThatExceptionOfType(MultipleFunctionsFoundException.class).isThrownBy(
                () -> runtime.run(p)
        );
    }

    @Test
    void noFunctionDefinitions() {
        SimpleRuntime runtime = SimpleRuntime.simpleLang();
        runtime.register(getVariable("y", new Object()));
        runtime.register(getVariable("x", new Object()));
        Program p = new Program(List.of(
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        assertThatExceptionOfType(NoFunctionFoundException.class).isThrownBy(
                () -> runtime.run(p)
        );
    }

    private static Variable getVariable(final String name, final Object value) {
        return new Variable() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public EvaluatedExpression value() {
                return new EvaluatedExpression.Literal(
                        value,
                        type(value.getClass())
                );
            }
        };
    }
}