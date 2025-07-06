package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.EvaluatedExpression;
import de.flogehring.peel.core.eval.EvaluatedProgram;
import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.Variable;
import de.flogehring.peel.core.lang.CodeElement;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.types.Bool;
import de.flogehring.peel.core.types.Number;
import de.flogehring.peel.core.types.PeelTypes;
import de.flogehring.peel.core.types.Text;
import de.flogehring.peel.core.values.PeelValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SimpleRuntimeTest {

    @Test
    void simple() {
        Program p = new Program(List.of(
                CodeElement.assign("x", CodeElement.integer(1)),
                CodeElement.assign("y", CodeElement.integer(1)),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        PeelValue value = runtime.run(p).getLastExpression().value();
        Assertions.assertEquals(2, value.value());
    }

    @Test
    void stringAddition() {
        Program p = new Program(List.of(
                CodeElement.assign("x", CodeElement.string("1")),
                CodeElement.assign("y", CodeElement.string("1")),
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        PeelValue value = runtime.run(p).getLastExpression().value();
        Assertions.assertEquals("11", value.value());
    }

    @Test
    void registerVariables() {
        Program p = new Program(List.of(
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        runtime.register(getVariable("x", "1"));
        runtime.register(getVariable("y", "2"));
        PeelValue value = runtime.run(p).getLastExpression().value();
        Assertions.assertEquals("12", value.value());
    }

    @Test
    void registerOperator() {
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        runtime.register(new Function() {
            @Override
            public String name() {
                return "*";
            }

            @Override
            public List<PeelTypes> arguments() {
                return List.of(new Text(), new Number.Integer());
            }

            @Override
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                EvaluatedExpression argumentLhs = arguments[0];
                EvaluatedExpression argumentRhs = arguments[1];
                String lhs = (String) argumentLhs.value().value();
                int rhs = (Integer) argumentRhs.value().value();
                return new EvaluatedExpression.BinaryOperator(
                        "*", new PeelValue.Primitive(new Text(), repeatString(lhs, rhs)), argumentLhs, argumentRhs
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
        runtime.register(integerVariable("y", 2));
        assertThat(runtime.run(p).getLastExpression().value().value()).isEqualTo("Echo!Echo!");
    }

    @Test
    void functionCall() {
        Program p = new Program(List.of(
                new Expression.FunctionCall(
                        "count",
                        List.of(new Expression.Literal(new PeelValue.Primitive(new Text(), "hello")),
                                new Expression.Literal(new PeelValue.Primitive(new Text(), "l"))
                        )))
        );
        EvaluatedProgram evaluatedProgram = RuntimeFactory.defaultLanguage().run(p);
        PeelValue value = evaluatedProgram.getLastExpression().value();
        assertThat(value.value()).isEqualTo(2);
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
            public List<PeelTypes> arguments() {
                return List.of(new Number.Integer(), new Number.Integer());
            }

            @Override
            public EvaluatedExpression run(EvaluatedExpression... arguments) {
                EvaluatedExpression argumentLhs = arguments[0];
                EvaluatedExpression argumentRhs = arguments[1];
                Integer lhs = (Integer) argumentLhs.value().value();
                Integer rhs = (Integer) argumentRhs.value().value();
                return new EvaluatedExpression.BinaryOperator(
                        "+", new PeelValue.Primitive(new Number.Integer(), lhs + rhs), argumentLhs, argumentRhs
                );
            }
        });
        runtime.register(integerVariable("y", 2));
        runtime.register(integerVariable("x", 1));
        Program p = new Program(List.of(
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
        ));
        assertThatExceptionOfType(MultipleFunctionsFoundException.class).isThrownBy(
                () -> runtime.run(p)
        );
    }

    @Test
    void noFunctionDefinitions() {
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
        runtime.register(boolVariable("y",false));
        runtime.register(boolVariable("x", true));
        Program p = new Program(List.of(
                CodeElement.expr(CodeElement.var("x"), "+", CodeElement.var("y"))
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
            public EvaluatedExpression value() {
                return new EvaluatedExpression.Literal(new PeelValue.Primitive(new Number.Integer(), value));
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
            public EvaluatedExpression value() {
                return new EvaluatedExpression.Literal(new PeelValue.Primitive(new Bool(), value));
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
            public EvaluatedExpression value() {
                return new EvaluatedExpression.Literal(
                        new PeelValue.Primitive(
                                new Text(),
                                value
                        )
                );
            }
        };
    }
}