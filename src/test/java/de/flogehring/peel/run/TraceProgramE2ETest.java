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
            // FunctionCall[name=fib, value=IntegerValue[value=2], arguments=[Literal[value=IntegerValue[value=3]]], subEvaluation=Optional[FunctionExecutionTrace[parameterBindings=[ParameterBinding[name=n, argument=Literal[value=IntegerValue[value=3]]]], bodyEvaluation=Block[content=[IfStatement[conditions=[BinaryOperator[operator===, value=BoolValue[value=false], lhs=VariableName[name=n, value=IntegerValue[value=3]], rhs=Literal[value=IntegerValue[value=1]], didShortCircuit=false], BinaryOperator[operator===, value=BoolValue[value=false], lhs=VariableName[name=n, value=IntegerValue[value=3]], rhs=Literal[value=IntegerValue[value=2]], didShortCircuit=false]], executedBlock=Block[content=[ReturnExpr[value=IntegerValue[value=2], expression=BinaryOperator[operator=+, value=IntegerValue[value=2], lhs=FunctionCall[name=fib, value=IntegerValue[value=1], arguments=[BinaryOperator[operator=-, value=IntegerValue[value=2], lhs=VariableName[name=n, value=IntegerValue[value=3]], rhs=Literal[value=IntegerValue[value=1]], didShortCircuit=false]], subEvaluation=Optional[FunctionExecutionTrace[parameterBindings=[ParameterBinding[name=n, argument=BinaryOperator[operator=-, value=IntegerValue[value=2], lhs=VariableName[name=n, value=IntegerValue[value=3]], rhs=Literal[value=IntegerValue[value=1]], didShortCircuit=false]]], bodyEvaluation=Block[content=[IfStatement[conditions=[BinaryOperator[operator===, value=BoolValue[value=false], lhs=VariableName[name=n, value=IntegerValue[value=2]], rhs=Literal[value=IntegerValue[value=1]], didShortCircuit=false], BinaryOperator[operator===, value=BoolValue[value=true], lhs=VariableName[name=n, value=IntegerValue[value=2]], rhs=Literal[value=IntegerValue[value=2]], didShortCircuit=false]], executedBlock=Block[content=[ReturnExpr[value=IntegerValue[value=1], expression=Literal[value=IntegerValue[value=1]]]]]]]]]]], rhs=FunctionCall[name=fib, value=IntegerValue[value=1], arguments=[BinaryOperator[operator=-, value=IntegerValue[value=1], lhs=VariableName[name=n, value=IntegerValue[value=3]], rhs=Literal[value=IntegerValue[value=2]], didShortCircuit=false]], subEvaluation=Optional[FunctionExecutionTrace[parameterBindings=[ParameterBinding[name=n, argument=BinaryOperator[operator=-, value=IntegerValue[value=1], lhs=VariableName[name=n, value=IntegerValue[value=3]], rhs=Literal[value=IntegerValue[value=2]], didShortCircuit=false]]], bodyEvaluation=Block[content=[IfStatement[conditions=[BinaryOperator[operator===, value=BoolValue[value=true], lhs=VariableName[name=n, value=IntegerValue[value=1]], rhs=Literal[value=IntegerValue[value=1]], didShortCircuit=false]], executedBlock=Block[content=[ReturnExpr[value=IntegerValue[value=1], expression=Literal[value=IntegerValue[value=1]]]]]]]]]]], didShortCircuit=false]]]]]]]]]]
            assertThat(trace.expressions().get(1)).isEqualTo(
                    new TraceExpression.FunctionCall(
                            "fib",
                            new TraceValue.IntegerValue(2),
                            List.of(new TraceExpression.Literal(new TraceValue.IntegerValue(3))),
                            Optional.of(new TraceExpression.FunctionExecutionTrace(
                                    List.of(
                                            new TraceExpression.ParameterBinding(
                                                    "n",
                                                    new TraceExpression.Literal(new TraceValue.IntegerValue(3))
                                            )
                                    ),
                                    new TraceExpression.Block(
                                            List.of(
                                                    new TraceExpression.IfStatement(
                                                            List.of(
                                                                    fibEqualityComparison(3, 1),
                                                                    fibEqualityComparison(3, 2)
                                                            ),
                                                            new TraceExpression.Block(
                                                                    List.of(
                                                                            new TraceExpression.ReturnExpr(
                                                                                    new TraceValue.IntegerValue(3),
                                                                                    null
                                                                            )
                                                                    )
                                                            )
                                                    )
                                            )
                                    )
                            ))
                    ));
        }

        private static TraceExpression.BinaryOperator fibEqualityComparison(int nValue, int comparisonValue) {
            return new TraceExpression.BinaryOperator(
                    "==",
                    new TraceValue.BoolValue(false),
                    new TraceExpression.VariableName("n", new TraceValue.IntegerValue(nValue)),
                    new TraceExpression.Literal(new TraceValue.IntegerValue(comparisonValue)),
                    false
            );
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
