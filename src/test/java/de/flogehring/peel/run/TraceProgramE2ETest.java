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

import static org.assertj.core.api.Assertions.assertThat;

class TraceProgramE2ETest {

    @Test
    void runtimeReturnsTraceProgramWithResult() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(PeelGrammar.parse("""
                var x = 1;
                x + 2;
                """));

        TraceProgram expected = new TraceProgram(
                List.of(
                        new TraceExpression.Assignment("x", intLiteral(1), intValue(1)),
                        new TraceExpression.BinaryOperator(
                                "+",
                                intValue(3),
                                var("x", 1),
                                intLiteral(2),
                                false
                        )
                ),
                intValue(3)
        );

        assertThat(trace).isEqualTo(expected);
    }

    @Test
    void logicalOrShortCircuitIsTraced() {
        PeelRuntime runtime = RuntimeFactory.defaultLanguage();
        TraceProgram trace = runtime.run(PeelGrammar.parse("True || unknownVar;"));

        TraceProgram expected = new TraceProgram(
                List.of(
                        new TraceExpression.BinaryOperator(
                                "||",
                                boolValue(true),
                                boolLiteral(true),
                                null,
                                true
                        )
                ),
                boolValue(true)
        );

        assertThat(trace).isEqualTo(expected);
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

        TraceProgram expected = new TraceProgram(
                List.of(
                        new TraceExpression.Assignment("x", intLiteral(0), intValue(0)),
                        new TraceExpression.WhileLoop(
                                List.of(
                                        new TraceExpression.WhileLoop.Iteration(
                                                new TraceExpression.UnaryPrefixOperator(
                                                        "!",
                                                        boolValue(true),
                                                        nEquals(0, 2)
                                                ),
                                                new TraceExpression.Block(List.of(
                                                        new TraceExpression.Assignment(
                                                                "x",
                                                                new TraceExpression.BinaryOperator(
                                                                        "+",
                                                                        intValue(1),
                                                                        var("x", 0),
                                                                        intLiteral(1),
                                                                        false
                                                                ),
                                                                intValue(1)
                                                        )
                                                ))
                                        ),
                                        new TraceExpression.WhileLoop.Iteration(
                                                new TraceExpression.UnaryPrefixOperator(
                                                        "!",
                                                        boolValue(true),
                                                        nEquals(1, 2)
                                                ),
                                                new TraceExpression.Block(List.of(
                                                        new TraceExpression.Assignment(
                                                                "x",
                                                                new TraceExpression.BinaryOperator(
                                                                        "+",
                                                                        intValue(2),
                                                                        var("x", 1),
                                                                        intLiteral(1),
                                                                        false
                                                                ),
                                                                intValue(2)
                                                        )
                                                ))
                                        ),
                                        new TraceExpression.WhileLoop.Iteration(
                                                new TraceExpression.UnaryPrefixOperator(
                                                        "!",
                                                        boolValue(false),
                                                        nEquals(2, 2)
                                                ),
                                                TraceExpression.EMPTY_BLOCK
                                        )
                                ),
                                intValue(2)
                        )
                ),
                intValue(2)
        );

        assertThat(trace).isEqualTo(expected);
    }

    private static TraceExpression.BinaryOperator nEquals(int nValue, int comparisonValue) {
        return new TraceExpression.BinaryOperator(
                "==",
                boolValue(nValue == comparisonValue),
                var("x", nValue),
                intLiteral(comparisonValue),
                false
        );
    }

    private static TraceExpression.VariableName var(String name, int value) {
        return new TraceExpression.VariableName(name, intValue(value));
    }

    private static TraceExpression.Literal intLiteral(int value) {
        return new TraceExpression.Literal(intValue(value));
    }

    private static TraceExpression.Literal boolLiteral(boolean value) {
        return new TraceExpression.Literal(boolValue(value));
    }

    private static TraceValue.IntegerValue intValue(int value) {
        return new TraceValue.IntegerValue(value);
    }

    private static TraceValue.BoolValue boolValue(boolean value) {
        return new TraceValue.BoolValue(value);
    }

    @Nested
    class Recursion {

        private static final String ANON_EMPTY_1 = "<anonymous_function>@1:9";
        private static final String ANON_EMPTY_2 = "<anonymous_function>@2:9";
        private static final String ANON_F1 = "<anonymous_function>@4:5";
        private static final String ANON_F2 = "<anonymous_function>@11:5";

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

            TraceProgram expected = new TraceProgram(
                    List.of(
                            functionDeclaration("fib", List.of("n")),
                            fibFunctionCall(3, intLiteral(3))
                    ),
                    intValue(2)
            );

            assertThat(trace).isEqualTo(expected);
        }

        @Test
        @Timeout(10)
        void recursiveWithOuterScope() {
            PeelRuntime runtime = RuntimeFactory.defaultLanguage();
            TraceProgram trace = runtime.run(PeelGrammar.parse(
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
                            """
            ));

            TraceProgram expected = new TraceProgram(
                    List.of(
                            new TraceExpression.Assignment("endCondition1", intLiteral(1), intValue(1)),
                            new TraceExpression.Assignment("endCondition2", intLiteral(2), intValue(2)),
                            functionDeclaration("fib", List.of("n")),
                            fibWithOuterScopeCall(5, intLiteral(5))
                    ),
                    intValue(5)
            );

            assertThat(trace).isEqualTo(expected);
        }

        @Test
        @Timeout(10)
        void anotherTest() {
            PeelRuntime runtime = RuntimeFactory.defaultLanguage();
            TraceProgram trace = runtime.run(PeelGrammar.parse(
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
                            """
            ));

            TraceProgram expected = new TraceProgram(
                    List.of(
                            new TraceExpression.Assignment("ONE", intLiteral(1), intValue(1)),
                            functionDeclaration("minus1", List.of("a")),
                            functionDeclaration("factorial", List.of("a")),
                            new TraceExpression.Assignment("a", intLiteral(3), intValue(3)),
                            new TraceExpression.BinaryOperator(
                                    "+",
                                    intValue(9),
                                    factorialWithMinus1Call(3, var("a", 3)),
                                    var("a", 3),
                                    false
                            )
                    ),
                    intValue(9)
            );

            assertThat(trace).isEqualTo(expected);
        }

        @Test
        @Timeout(10)
        void mutualRecursiveFactorial() {
            PeelRuntime runtime = RuntimeFactory.defaultLanguage();
            TraceProgram trace = runtime.run(PeelGrammar.parse(
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
                            """
            ));

            TraceProgram expected = new TraceProgram(
                    List.of(
                            new TraceExpression.Assignment(
                                    "f1",
                                    callableLiteral("closure", ANON_EMPTY_1, List.of()),
                                    new TraceValue.CallableRef("closure", ANON_EMPTY_1, List.of())
                            ),
                            new TraceExpression.Assignment(
                                    "f2",
                                    callableLiteral("closure", ANON_EMPTY_2, List.of()),
                                    new TraceValue.CallableRef("closure", ANON_EMPTY_2, List.of())
                            ),
                            new TraceExpression.Assignment(
                                    "f1",
                                    callableLiteral("closure", ANON_F1, List.of("x")),
                                    new TraceValue.CallableRef("closure", ANON_F1, List.of("x"))
                            ),
                            new TraceExpression.Assignment(
                                    "f2",
                                    callableLiteral("closure", ANON_F2, List.of("x")),
                                    new TraceValue.CallableRef("closure", ANON_F2, List.of("x"))
                            ),
                            new TraceExpression.Assignment("x", intLiteral(5), intValue(5)),
                            mutualFactorialCall(5, intLiteral(5), true)
                    ),
                    intValue(120)
            );

            assertThat(trace).isEqualTo(expected);
        }

        private static TraceExpression.FunctionCall fibFunctionCall(int n, TraceExpression argument) {
            return new TraceExpression.FunctionCall(
                    "fib",
                    intValue(fibValue(n)),
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
                return new TraceExpression.ReturnExpr(intValue(1), intLiteral(1));
            }

            TraceExpression leftArgument = fibSubtractionArgument(n, 1);
            TraceExpression rightArgument = fibSubtractionArgument(n, 2);

            TraceExpression sum = new TraceExpression.BinaryOperator(
                    "+",
                    intValue(fibValue(n)),
                    fibFunctionCall(n - 1, leftArgument),
                    fibFunctionCall(n - 2, rightArgument),
                    false
            );

            return new TraceExpression.ReturnExpr(intValue(fibValue(n)), sum);
        }

        private static TraceExpression.BinaryOperator fibSubtractionArgument(int n, int decrement) {
            return new TraceExpression.BinaryOperator(
                    "-",
                    intValue(n - decrement),
                    new TraceExpression.VariableName("n", intValue(n)),
                    intLiteral(decrement),
                    false
            );
        }

        private static TraceExpression.BinaryOperator fibEqualityComparison(int nValue, int comparisonValue) {
            return new TraceExpression.BinaryOperator(
                    "==",
                    boolValue(nValue == comparisonValue),
                    new TraceExpression.VariableName("n", intValue(nValue)),
                    intLiteral(comparisonValue),
                    false
            );
        }

        private static TraceExpression.FunctionCall fibWithOuterScopeCall(int n, TraceExpression argument) {
            return new TraceExpression.FunctionCall(
                    "fib",
                    intValue(fibValue(n)),
                    List.of(argument),
                    Optional.of(new TraceExpression.FunctionExecutionTrace(
                            List.of(new TraceExpression.ParameterBinding("n", argument)),
                            new TraceExpression.Block(List.of(fibWithOuterScopeIfStatement(n)))
                    ))
            );
        }

        private static TraceExpression.IfStatement fibWithOuterScopeIfStatement(int n) {
            return new TraceExpression.IfStatement(
                    n == 1
                            ? List.of(fibOuterComparison(n, "endCondition1", 1))
                            : List.of(fibOuterComparison(n, "endCondition1", 1), fibOuterComparison(n, "endCondition2", 2)),
                    new TraceExpression.Block(List.of(fibWithOuterScopeReturn(n)))
            );
        }

        private static TraceExpression.BinaryOperator fibOuterComparison(int nValue, String conditionName, int conditionValue) {
            return new TraceExpression.BinaryOperator(
                    "==",
                    boolValue(nValue == conditionValue),
                    new TraceExpression.VariableName("n", intValue(nValue)),
                    new TraceExpression.VariableName(conditionName, intValue(conditionValue)),
                    false
            );
        }

        private static TraceExpression.ReturnExpr fibWithOuterScopeReturn(int n) {
            if (n <= 2) {
                return new TraceExpression.ReturnExpr(intValue(1), intLiteral(1));
            }

            TraceExpression leftArgument = fibSubtractionArgument(n, 1);
            TraceExpression rightArgument = fibSubtractionArgument(n, 2);

            TraceExpression sum = new TraceExpression.BinaryOperator(
                    "+",
                    intValue(fibValue(n)),
                    fibWithOuterScopeCall(n - 1, leftArgument),
                    fibWithOuterScopeCall(n - 2, rightArgument),
                    false
            );

            return new TraceExpression.ReturnExpr(intValue(fibValue(n)), sum);
        }

        private static TraceExpression.FunctionCall factorialWithMinus1Call(int aValue, TraceExpression argument) {
            return new TraceExpression.FunctionCall(
                    "factorial",
                    intValue(factorialValue(aValue)),
                    List.of(argument),
                    Optional.of(new TraceExpression.FunctionExecutionTrace(
                            List.of(new TraceExpression.ParameterBinding("a", argument)),
                            factorialBody(aValue)
                    ))
            );
        }

        private static TraceExpression.Block factorialBody(int aValue) {
            if (aValue == 1) {
                return new TraceExpression.Block(List.of(factorialBaseCaseIf()));
            }

            TraceExpression recursiveArgument = minus1Call(aValue, new TraceExpression.VariableName("a", intValue(aValue)));
            TraceExpression multiplication = new TraceExpression.BinaryOperator(
                    "*",
                    intValue(factorialValue(aValue)),
                    new TraceExpression.VariableName("a", intValue(aValue)),
                    factorialWithMinus1Call(aValue - 1, recursiveArgument),
                    false
            );

            return new TraceExpression.Block(List.of(factorialNonBaseCaseIf(aValue), multiplication));
        }

        private static TraceExpression.IfStatement factorialBaseCaseIf() {
            return new TraceExpression.IfStatement(
                    List.of(
                            new TraceExpression.BinaryOperator(
                                    "==",
                                    boolValue(true),
                                    new TraceExpression.VariableName("a", intValue(1)),
                                    new TraceExpression.VariableName("ONE", intValue(1)),
                                    false
                            )
                    ),
                    new TraceExpression.Block(List.of(
                            new TraceExpression.ReturnExpr(intValue(1), new TraceExpression.VariableName("ONE", intValue(1)))
                    ))
            );
        }

        private static TraceExpression.IfStatement factorialNonBaseCaseIf(int aValue) {
            return new TraceExpression.IfStatement(
                    List.of(
                            new TraceExpression.BinaryOperator(
                                    "==",
                                    boolValue(false),
                                    new TraceExpression.VariableName("a", intValue(aValue)),
                                    new TraceExpression.VariableName("ONE", intValue(1)),
                                    false
                            )
                    ),
                    null
            );
        }

        private static TraceExpression.FunctionCall minus1Call(int aValue, TraceExpression argument) {
            return new TraceExpression.FunctionCall(
                    "minus1",
                    intValue(aValue - 1),
                    List.of(argument),
                    Optional.of(new TraceExpression.FunctionExecutionTrace(
                            List.of(new TraceExpression.ParameterBinding("a", argument)),
                            new TraceExpression.Block(List.of(
                                    new TraceExpression.BinaryOperator(
                                            "-",
                                            intValue(aValue - 1),
                                            new TraceExpression.VariableName("a", intValue(aValue)),
                                            new TraceExpression.VariableName("ONE", intValue(1)),
                                            false
                                    )
                            ))
                    ))
            );
        }

        private static TraceExpression.FunctionCall mutualFactorialCall(int xValue, TraceExpression argument, boolean isF1) {
            String currentName = isF1 ? ANON_F1 : ANON_F2;

            return new TraceExpression.FunctionCall(
                    currentName,
                    intValue(factorialValue(xValue)),
                    List.of(argument),
                    Optional.of(new TraceExpression.FunctionExecutionTrace(
                            List.of(new TraceExpression.ParameterBinding("x", argument)),
                            mutualFactorialBody(xValue, isF1)
                    ))
            );
        }

        private static TraceExpression.Block mutualFactorialBody(int xValue, boolean isF1) {
            if (xValue == 1) {
                return new TraceExpression.Block(List.of(
                        new TraceExpression.IfStatement(
                                List.of(
                                        new TraceExpression.BinaryOperator(
                                                "==",
                                                boolValue(true),
                                                new TraceExpression.VariableName("x", intValue(1)),
                                                intLiteral(1),
                                                false
                                        )
                                ),
                                new TraceExpression.Block(List.of(
                                        new TraceExpression.ReturnExpr(intValue(1), new TraceExpression.VariableName("x", intValue(1)))
                                ))
                        )
                ));
            }

            TraceExpression subtraction = new TraceExpression.BinaryOperator(
                    "-",
                    intValue(xValue - 1),
                    new TraceExpression.VariableName("x", intValue(xValue)),
                    intLiteral(1),
                    false
            );

            TraceExpression recursive = mutualFactorialCall(xValue - 1, subtraction, !isF1);

            return new TraceExpression.Block(List.of(
                    new TraceExpression.IfStatement(
                            List.of(
                                    new TraceExpression.BinaryOperator(
                                            "==",
                                            boolValue(false),
                                            new TraceExpression.VariableName("x", intValue(xValue)),
                                            intLiteral(1),
                                            false
                                    )
                            ),
                            null
                    ),
                    new TraceExpression.BinaryOperator(
                            "*",
                            intValue(factorialValue(xValue)),
                            new TraceExpression.VariableName("x", intValue(xValue)),
                            recursive,
                            false
                    )
            ));
        }

        private static TraceExpression.Literal functionDeclaration(String name, List<String> arities) {
            return callableLiteral("function", name, arities);
        }

        private static TraceExpression.Literal callableLiteral(String kind, String name, List<String> arities) {
            return new TraceExpression.Literal(new TraceValue.CallableRef(kind, name, arities));
        }

        private static int fibValue(int n) {
            if (n <= 2) {
                return 1;
            }
            return fibValue(n - 1) + fibValue(n - 2);
        }

        private static int factorialValue(int n) {
            if (n <= 1) {
                return 1;
            }
            return n * factorialValue(n - 1);
        }
    }
}
