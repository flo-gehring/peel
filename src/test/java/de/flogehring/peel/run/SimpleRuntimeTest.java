package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.Variable;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.ExpressionFactoryMethods;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Bool;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.core.values.Text;
import de.flogehring.peel.run.exceptions.MultipleFunctionsFoundException;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.flogehring.peel.core.values.PeelValue.integer;
import static de.flogehring.peel.core.values.PeelValue.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SimpleRuntimeTest {

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
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
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
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        PeelValue value = runtime.run(p).getLastExpression().value();
        Assertions.assertEquals(text("11"), value);
    }

    @Test
    void registerVariables() {
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "+", ExpressionFactoryMethods.var("y"))
        ));
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        runtime.register(getVariable("x", "1"));
        runtime.register(getVariable("y", "2"));
        PeelValue value = runtime.run(p).getLastExpression().value();
        Assertions.assertEquals(text("12"), value);
    }

    @Test
    void registerOperator() {
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
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
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                EvaluatedExpression argumentLhs = arguments[0];
                EvaluatedExpression argumentRhs = arguments[1];
                String lhs = ((Text) argumentLhs.value()).value();
                int rhs = ((Number.Integer) argumentRhs.value()).value();
                return new EvaluatedExpression.BinaryOperator(
                        "*", new Text(repeatString(lhs, rhs)), argumentLhs, argumentRhs
                );
            }

            private String repeatString(String lhs, int rhs) {
                return lhs.repeat(rhs);
            }
        });
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "µ", ExpressionFactoryMethods.var("y"))
        ));
        runtime.register(getVariable("x", "Echo!"));
        runtime.register(integerVariable("y", 2));
        assertThat(runtime.run(p).getLastExpression().value()).isEqualTo(text("Echo!Echo!"));
    }

    @Test
    void functionCall() {
        Program p = new Program(List.of(
                new Expression.FunctionCall(
                        "count",
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
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
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
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                EvaluatedExpression argumentLhs = arguments[0];
                EvaluatedExpression argumentRhs = arguments[1];
                Number.Integer lhs = (Number.Integer) argumentLhs.value();
                Number.Integer rhs = (Number.Integer) argumentRhs.value();
                return new EvaluatedExpression.BinaryOperator(
                        "+", new Number.Integer(lhs.numberValue().add(rhs.numberValue()).intValue()), argumentLhs, argumentRhs
                );
            }
        });
        runtime.register(integerVariable("y", 2));
        runtime.register(integerVariable("x", 1));
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "+", ExpressionFactoryMethods.var("y"))
        ));
        assertThatExceptionOfType(MultipleFunctionsFoundException.class).isThrownBy(
                () -> runtime.run(p)
        );
    }

    @Test
    void noFunctionDefinitions() {
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        runtime.register(boolVariable("y", false));
        runtime.register(boolVariable("x", true));
        Program p = new Program(List.of(
                ExpressionFactoryMethods.expr(ExpressionFactoryMethods.var("x"), "+", ExpressionFactoryMethods.var("y"))
        ));
        assertThatExceptionOfType(NoFunctionFoundException.class).isThrownBy(
                () -> runtime.run(p)
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

    private static Variable boolVariable(
            String name,
            boolean value
    ) {
        return new Variable() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public PeelValue value() {
                return new Bool(value);
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