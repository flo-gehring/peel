package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.values.*;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.run.NoFunctionFoundException;
import de.flogehring.peel.run.PeelException;
import de.flogehring.peel.run.SimpleRuntime;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Stream;

import static de.flogehring.peel.convenience.FunctionFactory.binary;
import static de.flogehring.peel.run.SimpleRuntime.empty;

public class RuntimeFactory {

    private RuntimeFactory() {
    }

    /**
     * Register useful Operators and Functions to the Runtime.
     *
     * @return Prepopulated Runtime
     */
    public static SimpleRuntime defaultLanguage() {
        SimpleRuntime runtime = empty();
        runtime.register(add());
        runtime.register(countSubstring());
        return runtime;
    }

    private static Function add() {
        return binary(
                "+",
                (lhs, rhs) -> switch (lhs) {
                    case PeelValue.Collection cLhs -> switch (rhs) {
                        case PeelValue.Collection cRhs -> addCollections(cLhs, cRhs);
                        case Primitives _ -> throw new NoFunctionFoundException(
                                "Can't add Collection and Primitive Value"
                        );
                    };
                    case Primitives primitiveLhs -> switch (rhs) {
                        case PeelValue.Collection _ -> throw new NoFunctionFoundException(
                                "Can't add Primitive and Collection"
                        );
                        case Primitives primitiveRhs -> addPrimitives(
                                primitiveLhs,
                                primitiveRhs
                        );
                    };
                }
        );
    }

    private static PeelValue addPrimitives(
            Primitives primitiveLhs,
            Primitives primitiveRhs
    ) {
        return switch (primitiveLhs) {
            case Bool _ -> throw new NoFunctionFoundException("Cant add bool");
            case Number numberLhs -> {
                if (primitiveRhs instanceof Number numberRhs) {
                    yield addNumberPrimitives(numberLhs, numberRhs);
                } else {
                    throw new NoFunctionFoundException(
                            "Can't add number and {0}", primitiveRhs.getClass().getName()
                    );
                }
            }
            case Text(var textLhs) -> {
                if (primitiveRhs instanceof Text(var textRhs)) {
                    yield new Text(textLhs + textRhs);
                } else {
                    throw new NoFunctionFoundException(
                            "Can't add text and {0}", primitiveRhs.getClass().getName()
                    );
                }
            }
            case None _ -> throw new PeelException(
                    "Can't add None to number"
            );
        };

    }

    private static PeelValue addNumberPrimitives(Number numberLhs, Number numberRhs) {
        return switch (numberLhs) {
            case Number.Decimal(var lhsDecimal) -> new Number.Decimal(
                    lhsDecimal.add(numberRhs.numberValue())
            );
            case Number.Float(var floatLhs) -> {
                if (numberRhs instanceof Number.Float(var floatRhs)) {
                    yield new Number.Float(floatLhs + floatRhs);
                } else if (numberRhs instanceof Number.Integer(var intLhs)) {
                    yield new Number.Float(floatLhs + intLhs);
                } else {
                    yield new Number.Decimal(numberLhs.numberValue().add(numberRhs.numberValue()));
                }
            }
            case Number.Integer(var intLhs) -> switch (numberRhs) {
                case Number.Decimal(var decimalRhs) -> new Number.Decimal(BigDecimal.valueOf(intLhs).add(decimalRhs));
                case Number.Float(var floatRhs) -> new Number.Float(
                        intLhs + floatRhs
                );
                case Number.Integer(var intRhs) -> new Number.Integer(
                        intLhs + intRhs
                );
            };
        };

    }

    private static PeelValue addCollections(PeelValue.Collection cLhs, PeelValue.Collection cRhs) {
        return switch (cLhs) {
            case PeelValue.Collection.List(var listLhs) -> switch (cRhs) {
                case PeelValue.Collection.List(var listRhs) -> PeelValue.list(
                        Stream.concat(
                                listLhs.stream(),
                                listRhs.stream()
                        ).toList()
                );
                case PeelValue.Collection.Map _ -> throw new PeelException(
                        "Can't add list and map"
                );
            };
            case PeelValue.Collection.Map(var mapLhs) -> {
                switch (cRhs) {
                    case PeelValue.Collection.List _ -> throw new PeelException(
                            "Can't add map and list"
                    );
                    case PeelValue.Collection.Map(var mapRhs) -> {
                        HashMap<Primitives, PeelValue> result = new HashMap<>(mapLhs);
                        result.putAll(mapRhs);
                        yield new PeelValue.Collection.Map(Collections.unmodifiableMap(result));
                    }
                }
            }
        };
    }

    private static Function countSubstring() {
        return binary(
                "count",
                (lhs, rhs) -> {
                    if (lhs instanceof Text(var stringLhs) && rhs instanceof Text(var stringRhs)) {
                        int occurences = 0;
                        while ((stringLhs).contains(stringRhs)) {
                            occurences++;
                            stringLhs = stringLhs.replaceFirst(stringRhs, "");
                        }
                        return new Number.Integer(occurences);
                    } else {
                        throw new NoFunctionFoundException("Can't count types {0} {1}", lhs.getClass().getName(), rhs.getClass().getName());
                    }
                }
        );
    }
}
