package de.flogehring.peel.run;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static de.flogehring.peel.core.values.PeelValue.integer;
import static de.flogehring.peel.run.TestHelpers.runProgrammAndExpect;

public class FunctionTest {

    @Test
    void simple() {
        runProgrammAndExpect(
                """
                        fun add(a,b) {
                            a + b;
                        }
                        
                        add(1,1);
                        """,
                integer(2)
        );
    }

    @Test
    void simpleWithReturn() {
        runProgrammAndExpect(
                """
                        fun add(a,b) {
                            return a + b;
                        }
                        add(1,2);
                        """,
                integer(3)
        );
    }

    @Test
    void sumList() {
        runProgrammAndExpect(
                """
                        fun sum(l) {
                            var s = 0;
                            for(e in l) {
                                s = s + e;
                            }
                        }
                        sum([1,2,3,4]);
                        """,
                integer(10)
        );
    }

    @Test
    void sumListWithVariableShadowing() {
        runProgrammAndExpect(
                """
                        var s = [1,2,3,4];
                        fun sum(l) {
                            var s = 0;
                            for(e in l) {
                                s = s + e;
                            }
                        }
                        sum(s);
                        """,
                integer(10)
        );
    }

    @Test
    void returnFromLoop() {
        runProgrammAndExpect(
                """
                        var s = [1,2,3,4];
                        fun indexOf(l, s) {
                            var i = 0;
                            for(e in l) {
                                if(e == s) {
                                    return i;
                                }
                                i = i + 1;
                            }
                            -1;
                        }
                        indexOf(s, 2);
                        """,
                integer(1)
        );
    }

    @Nested
    class Recursion {

        @ParameterizedTest
        @Timeout(10)
        @CsvSource(value = {
                "1,1",
                "2,1",
                "3,2",
                "4,3",
                "5,5",
                "6,8"
        })
        void recursive(int n, int expected) {
            runProgrammAndExpect(
                    String.format("""
                            fun fib(n) {
                                if (n == 1) {
                                    return 1;
                                } else if (n == 2) {
                                    return 1;
                                } else {
                                    return fib(n -1) + fib(n -2);
                                }
                            }
                            fib(%d);
                            """, n
                    ),
                    integer(expected)
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

    @Nested
    class Lambdas {

        @Test
        void asVariable() {
            runProgrammAndExpect(
                    """
                            var s = [1,2,3,4];
                            var f = fun (l) {
                                var s = 0;
                                for(e in l) {
                                    s = s + e;
                                }
                            };
                            f(s);
                            """,
                    integer(10)
            );
        }

        @Test
        void immediateCall() {
            runProgrammAndExpect(
                    """
                            fun(i) {i+1;}(3);
                            """,
                    integer(4)
            );
        }
    }

    @Nested
    class HigherOrderFunctions {

        @Test
        void globalFunctionAsArgument() {
            runProgrammAndExpect(
                    """
                            fun double(x) {
                                x * 2;
                            }
                            fun apply(f, x) {
                                f(x);
                            }
                            apply(double, 3);
                            """,
                    integer(6)
            );
        }

        @Test
        void variableAsArgument() {
            runProgrammAndExpect(
                    """
                            var double = fun (x) {
                                x * 2;
                            };
                            fun apply(f, x) {
                                f(x);
                            }
                            apply(double, 3);
                            """,
                    integer(6)
            );
        }

        @Test
        void lambdaAsArgument() {
            runProgrammAndExpect(
                    """
                            fun apply(f, x) {
                                f(x);
                            }
                            apply(fun(x) { x * 2; }, 3);
                            """,
                    integer(6)
            );
        }

    }

    @Nested
    class Closures {

        @Test
        void loopVariablesArePreserved() {
            runProgrammAndExpect(
                    """
                            var list = [];
                            for (i in [1,2,3,4]) {
                                list = list + fun(x) { x + i;};
                            }
                            var s = 0;
                            for (f in list) {
                                s = s + f(1);
                            }
                            """,
                    integer(14)
            );
        }

        @Test
        void closuresPlusRecursion() {
            runProgrammAndExpect(
                    """
                            var f1 = fun() {};
                            if (1==1) {
                                var x = 3;
                                f1 = fun(y) {
                                    if (y == x) {
                                        return x;
                                    }
                                    return y + f1(y -1);
                                };
                            }
                            var x = 5;
                            f(10);
                            """,
                    integer(45)
            );
        }
    }
}
