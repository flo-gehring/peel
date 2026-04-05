package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.*;
import de.flogehring.peel.core.eval.Runtime;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.ExpressionFactoryMethods;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Bool;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.core.values.Text;
import de.flogehring.peel.run.exceptions.AmbiguousOperatorException;
import de.flogehring.peel.run.exceptions.DuplicateRequestBindingException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.UndefinedVarException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.flogehring.peel.core.values.PeelValue.integer;
import static de.flogehring.peel.core.values.PeelValue.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SimpleRuntimeTest {

    private static SimpleRuntime newMutableRuntime() {
        return (SimpleRuntime) RuntimeFactory.defaultLanguage();
    }

    @Test
    void simple() {
        Program p = new Program(
                new Expression.Block(List.of(
                        ExpressionFactoryMethods.assign("x", ExpressionFactoryMethods.integer(1), 0),
                        ExpressionFactoryMethods.assign("y", ExpressionFactoryMethods.integer(1), 0),
                        ExpressionFactoryMethods.expr(
                                ExpressionFactoryMethods.var("x", 0),
                                "+",
                                ExpressionFactoryMethods.var("y", 0)
                        )
                )));
        Runtime runtime = RuntimeFactory.defaultLanguage();
        PeelValue value = runtime.run(p).getLastExpression().value();
        Assertions.assertEquals(PeelValue.integer(2), value);
    }

    @Test
    void stringAddition() {
        Program p = new Program(List.of(
                ExpressionFactoryMethods.assign("x", ExpressionFactoryMethods.string("1"), 0),
                ExpressionFactoryMethods.assign("y", ExpressionFactoryMethods.string("1"), 0),
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x", 0), "+", ExpressionFactoryMethods.var("y", 0))
        ));
        Runtime runtime = RuntimeFactory.defaultLanguage();
        PeelValue value = runtime.run(p).getLastExpression().value();
        Assertions.assertEquals(text("11"), value);
    }

    @Test
    void registerVariables() {
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "+", ExpressionFactoryMethods.var("y"))
        ));
        SimpleRuntime runtime = newMutableRuntime();
        runtime.register(getVariable("x", "1"));
        runtime.register(getVariable("y", "2"));
        PeelValue value = runtime.run(p).getLastExpression().value();
        Assertions.assertEquals(text("12"), value);
    }

    @Test
    void registerOperator() {
        SimpleRuntime runtime = newMutableRuntime();
        runtime.register(OperatorDef.typed(
                "µ",
                Text.class,
                Number.Integer.class,
                (lhs, rhs) -> new Text(((Text) lhs).value().repeat(((Number.Integer) rhs).value()))
        ));
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "µ", ExpressionFactoryMethods.var("y"))
        ));
        runtime.register(getVariable("x", "Echo!"));
        runtime.register(integerVariable("y", 2));
        assertThat(runtime.run(p).getLastExpression().value()).isEqualTo(text("Echo!Echo!"));
    }

    @Test
    void binaryFunctionIsNotImplicitOperator() {
        SimpleRuntime runtime = newMutableRuntime();
        runtime.register(new Function() {
            @Override
            public String name() {
                return "µ";
            }

            @Override
            public int arity() {
                return 2;
            }

            @Override
            public PeelValue run(EvaluatedExpression... arguments) {
                EvaluatedExpression argumentLhs = arguments[0];
                EvaluatedExpression argumentRhs = arguments[1];
                String lhs = ((Text) argumentLhs.value()).value();
                int rhs = ((Number.Integer) argumentRhs.value()).value();
                return new Text(lhs.repeat(rhs));
            }
        });

        runtime.register(getVariable("x", "Echo!"));
        runtime.register(integerVariable("y", 2));
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "µ", ExpressionFactoryMethods.var("y"))
        ));

        assertThatExceptionOfType(NoFunctionFoundException.class).isThrownBy(() -> runtime.run(p));
    }

    @Test
    void functionCall() {
        Program p = new Program(List.of(
                new Expression.FunctionCall(
                        new Expression.VariableName("count", -1),
                        List.of(new Expression.Literal(new Text("hello")),
                                new Expression.Literal(new Text("l"))
                        )))
        );
        EvaluatedProgram evaluatedProgram = RuntimeFactory.defaultLanguage().run(p);
        PeelValue value = evaluatedProgram.getLastExpression().value();
        assertThat(value).isEqualTo(integer(2));
    }

    @Test
    void multipleOperatorDefinitions() {
        SimpleRuntime runtime = newMutableRuntime();
        runtime.register(new Function() {
            @Override
            public String name() {
                return "+";
            }

            @Override
            public int arity() {
                return 2;
            }

            @Override
            public PeelValue run(EvaluatedExpression... arguments) {
                EvaluatedExpression argumentLhs = arguments[0];
                EvaluatedExpression argumentRhs = arguments[1];
                Number.Integer lhs = (Number.Integer) argumentLhs.value();
                Number.Integer rhs = (Number.Integer) argumentRhs.value();
                return new Number.Integer(lhs.numberValue().add(rhs.numberValue()).intValue());
            }
        });
        runtime.register(integerVariable("y", 2));
        runtime.register(integerVariable("x", 1));
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "+", ExpressionFactoryMethods.var("y"))
        ));
        assertThat(runtime.run(p).getLastExpression().value()).isEqualTo(integer(3));
    }

    @Test
    void ambiguousTypedOperatorDefinitions() {
        SimpleRuntime runtime = newMutableRuntime();
        runtime.register(OperatorDef.typed(
                "~",
                PeelValue.class,
                PeelValue.class,
                (lhs, rhs) -> text("a")
        ));
        runtime.register(OperatorDef.typed(
                "~",
                PeelValue.class,
                PeelValue.class,
                (lhs, rhs) -> text("b")
        ));
        runtime.register(integerVariable("x", 1));
        runtime.register(integerVariable("y", 2));
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "~", ExpressionFactoryMethods.var("y"))
        ));
        assertThatExceptionOfType(AmbiguousOperatorException.class).isThrownBy(
                () -> runtime.run(p)
        );
    }

    @Test
    void noFunctionDefinitions() {
        Runtime runtime = RuntimeFactory.defaultLanguage();
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "+", ExpressionFactoryMethods.var("y"))
        ));
        assertThatExceptionOfType(NoFunctionFoundException.class).isThrownBy(
                () -> runtime.run(
                        p,
                        RequestBindings.of(
                                Variable.of("x", new Bool(true)),
                                Variable.of("y", new Bool(false))
                        )
                )
        );
    }

    @Test
    void requestBindingsAreAvailablePerRun() {
        Runtime runtime = RuntimeFactory.defaultLanguage();
        Program program = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "+", ExpressionFactoryMethods.var("y"))
        ));

        EvaluatedProgram run = runtime.run(
                program,
                RequestBindings.of(
                        Variable.of("x", text("hello")),
                        Variable.of("y", text("world"))
                )
        );

        assertThat(run.getLastExpression().value()).isEqualTo(text("helloworld"));
    }

    @Test
    void requestBindingsDoNotLeakAcrossRuns() {
        Runtime runtime = RuntimeFactory.defaultLanguage();
        Program program = new Program(List.of(
                ExpressionFactoryMethods.var("x")
        ));

        EvaluatedProgram first = runtime.run(
                program,
                RequestBindings.of(Variable.of("x", text("first")))
        );
        assertThat(first.getLastExpression().value()).isEqualTo(text("first"));

        assertThatExceptionOfType(UndefinedVarException.class).isThrownBy(() -> runtime.run(program));
    }

    @Test
    void requestBindingConflictingWithGlobalVariableThrows() {
        SimpleRuntime runtime = newMutableRuntime();
        runtime.register(Variable.of("userId", text("global-user")));
        Program program = new Program(List.of(ExpressionFactoryMethods.var("userId")));

        assertThatExceptionOfType(DuplicateRequestBindingException.class).isThrownBy(
                () -> runtime.run(
                        program,
                        RequestBindings.of(Variable.of("userId", text("request-user")))
                )
        );
    }


    private static Variable integerVariable(
            String name,
            int value
    ) {
        return new Variable() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public PeelValue value() {
                return integer(value);
            }
        };
    }

    private static Variable getVariable(
            final String name,
            final String value
    ) {
        return new Variable() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public PeelValue value() {
                return new Text(value);
            }
        };
    }
}
