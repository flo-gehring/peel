package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeFactory;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.trace.TraceExpression;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.parse.PeelGrammar;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Optional;

import static de.flogehring.peel.core.values.PeelValue.integer;
import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;
import static org.assertj.core.api.Assertions.assertThat;

class TraceProgramE2ETest {

    @Test
    void runtimeReturnsTraceProgramWithResult() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(PeelGrammar.parse("""
                var x = 1;
                x + 2;
                """));

        assertThat(trace.expressions()).hasSize(2);
        assertThat(trace.result()).isEqualTo(new TraceValue.IntegerValue(3));
    }

    @Test
    void logicalOrShortCircuitIsTraced() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(PeelGrammar.parse("True || unknownVar;"));

        assertThat(trace.expressions()).hasSize(1);
        assertThat(trace.result()).isEqualTo(new TraceValue.BoolValue(true));
        assertThat(trace.expressions().getFirst()).isInstanceOf(TraceExpression.BinaryOperator.class);

        TraceExpression.BinaryOperator logical = (TraceExpression.BinaryOperator) trace.expressions().getFirst();
        assertThat(logical.didShortCircuit()).isTrue();
        assertThat(logical.rhs()).isNull();
    }

    @Test
    void whileLoopContainsTerminatingConditionIteration() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(PeelGrammar.parse("""
                var x = 0;
                while (!(x == 2)) {
                    x = x + 1;
                }
                """));

        assertThat(trace.expressions()).hasSize(2);
        assertThat(trace.expressions().get(1)).isInstanceOf(TraceExpression.WhileLoop.class);

        TraceExpression.WhileLoop loop = (TraceExpression.WhileLoop) trace.expressions().get(1);
        assertThat(loop.iterations()).hasSize(3);
        assertThat(loop.iterations().getLast().body()).isEqualTo(TraceExpression.EMPTY_BLOCK);
        assertThat(loop.value()).isEqualTo(new TraceValue.IntegerValue(2));
    }


    @Nested
    class Recursion {

        @Test
        @Timeout(10)
        void recursive() {
            PeelRuntime runtime = RuntimeFactory.defaultLanguage();
            TraceProgram trace = runtime.run(PeelGrammar.parse("""
                    fun fib(n) {
                        if (n == 1) {
                            return 1;
                        } else if (n == 2) {
                            return 1;
                        } else {
                            return fib(n -1) + fib(n -2);
                        }
                    }
                    fib(3);
                    """
            ));
            assertThat(trace.expressions()).hasSize(2);
            assertThat(trace.expressions().get(1)).isEqualTo(fibFunctionCall(3, fibLiteral(3)));
        }

        private static TraceExpression.FunctionCall fibFunctionCall(int n, TraceExpression argument) {
            return new TraceExpression.FunctionCall(
                    "fib",
                    fibIntegerValue(fibValue(n)),
                    List.of(argument),
                    Optional.of(new TraceExpression.FunctionExecutionTrace(
                            List.of(new TraceExpression.ParameterBinding("n", argument)),
                            new TraceExpression.Block(List.of(fibIfStatement(n)))
                    ))
            );
        }

        private static TraceExpression.IfStatement fibIfStatement(int n) {
            return new TraceExpression.IfStatement(
                    n == 1
                            ? List.of(fibEqualityComparison(n, 1))
                            : List.of(fibEqualityComparison(n, 1), fibEqualityComparison(n, 2)),
                    new TraceExpression.Block(List.of(fibReturnExpr(n)))
            );
        }

        private static TraceExpression.ReturnExpr fibReturnExpr(int n) {
            if (n <= 2) {
                return new TraceExpression.ReturnExpr(fibIntegerValue(1), fibLiteral(1));
            }

            TraceExpression leftArgument = fibSubtractionArgument(n, 1);
            TraceExpression rightArgument = fibSubtractionArgument(n, 2);

            TraceExpression sum = new TraceExpression.BinaryOperator(
                    "+",
                    fibIntegerValue(fibValue(n)),
                    fibFunctionCall(n - 1, leftArgument),
                    fibFunctionCall(n - 2, rightArgument),
                    false
            );

            return new TraceExpression.ReturnExpr(fibIntegerValue(fibValue(n)), sum);
        }

        private static TraceExpression.BinaryOperator fibSubtractionArgument(int n, int decrement) {
            return new TraceExpression.BinaryOperator(
                    "-",
                    fibIntegerValue(n - decrement),
                    new TraceExpression.VariableName("n", fibIntegerValue(n)),
                    fibLiteral(decrement),
                    false
            );
        }

        private static TraceExpression.BinaryOperator fibEqualityComparison(int nValue, int comparisonValue) {
            return new TraceExpression.BinaryOperator(
                    "==",
                    new TraceValue.BoolValue(nValue == comparisonValue),
                    new TraceExpression.VariableName("n", fibIntegerValue(nValue)),
                    fibLiteral(comparisonValue),
                    false
            );
        }

        private static TraceExpression.Literal fibLiteral(int value) {
            return new TraceExpression.Literal(fibIntegerValue(value));
        }

        private static TraceValue.IntegerValue fibIntegerValue(int value) {
            return new TraceValue.IntegerValue(value);
        }

        private static int fibValue(int n) {
            if (n <= 2) {
                return 1;
            }
            return fibValue(n - 1) + fibValue(n - 2);
        }

        @Test
        @Timeout(10)
        void recursiveWithOuterScope() {
            runProgrammAndExpect(
                    """
                            var endCondition1 = 1;
                            var endCondition2 = 2;
                            fun fib(n) {
                                if (n == endCondition1) {
                                    return 1;
                                } else if (n == endCondition2) {
                                    return 1;
                                } else {
                                    return fib(n -1) + fib(n -2);
                                }
                            }
                            fib(5);
                            """,
                    integer(5)
            );
        }

        @Test
        @Timeout(10)
        void anotherTest() {
            runProgrammAndExpect(
                    """
                            var ONE = 1;
                            fun minus1(a) {
                                a - ONE;
                            }
                            
                            fun factorial(a) {
                                if (a == ONE) {
                                    return ONE;
                                }
                                a * factorial(minus1(a))
                            }
                            var a = 3;
                            factorial(a) + a;
                            """,
                    integer(9)
            );
        }

        @Test
        @Timeout(10)
        void mutualRecursiveFactorial() {
            runProgrammAndExpect(
                    """
                            var f1 = fun() {};
                            var f2 = fun() {};
                            
                            f1 = fun(x) {
                             if (x==1) {
                                return x;
                             }
                              x * f2(x -1);
                            };
                            
                            f2 = fun(x) {
                             if (x==1) {
                                return x;
                             }
                              x * f1(x -1);
                            };
                            var x =5;
                            f1(5);
                            """,
                    integer(120)
            );
        }
    }
}
