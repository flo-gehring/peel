package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.eval.RuntimeModule;
import de.flogehring.peel.core.values.*;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.PeelException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static de.flogehring.peel.convenience.FunctionFactory.binary;

final class StandardRuntimeModule {

    static final RuntimeModule INSTANCE = RuntimeModule.of(
            List.of(),
            List.of(countSubstring()),
            List.of(
                    OperatorDef.typed(
                            "+",
                            PeelValue.Collection.class,
                            PeelValue.Collection.class,
                            (lhs, rhs) -> addCollections((PeelValue.Collection) lhs, (PeelValue.Collection) rhs)
                    ),
                    OperatorDef.typed(
                            "+",
                            PeelValue.Collection.class,
                            Primitives.class,
                            (lhs, rhs) -> addCollectionAnd((PeelValue.Collection) lhs, (Primitives) rhs)
                    ),
                    OperatorDef.typed(
                            "+",
                            PeelValue.Collection.class,
                            PeelCallable.class,
                            (lhs, rhs) -> addCollectionAnd((PeelValue.Collection) lhs, (PeelCallable) rhs)
                    ),
                    OperatorDef.typed(
                            "+",
                            Text.class,
                            Text.class,
                            (lhs, rhs) -> new Text(((Text) lhs).value() + ((Text) rhs).value())
                    ),
                    OperatorDef.typed(
                            "-",
                            PeelValue.Collection.class,
                            PeelValue.Collection.class,
                            (lhs, rhs) -> subCollection((PeelValue.Collection) lhs, (PeelValue.Collection) rhs)
                    ),
                    OperatorDef.typed(
                            "-",
                            Text.class,
                            Text.class,
                            (lhs, rhs) -> new Text(((Text) lhs).value().replace(((Text) rhs).value(), ""))
                    ),
                    OperatorDef.typed(
                            "==",
                            PeelValue.class,
                            PeelValue.class,
                            (lhs, rhs) -> PeelValue.bool(lhs.equals(rhs))
                    ),
                    OperatorDef.typed(
                            "!",
                            Bool.class,
                            value -> PeelValue.bool(!((Bool) value).value())
                    )
            )
    );

    private StandardRuntimeModule() {
    }

    private static PeelValue subCollection(PeelValue.Collection cLhs, PeelValue.Collection cRhs) {
        return switch (cLhs) {
            case PeelValue.Collection.List(var listLhs) -> switch (cRhs) {
                case PeelValue.Collection.List(var listRhs) -> PeelValue.list(
                        listLhs.stream().filter(
                                valL -> !listRhs.contains(valL)
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
                        for (Primitives key : mapRhs.keySet()) {
                            result.remove(key);
                        }
                        yield new PeelValue.Collection.Map(Collections.unmodifiableMap(result));
                    }
                }
            }
        };
    }

    private static <T extends PeelValue> PeelValue addCollectionAnd(PeelValue.Collection cLhs, T pRhs) {
        if (cLhs instanceof PeelValue.Collection.List(List<PeelValue> list)) {
            return new PeelValue.Collection.List(
                    Stream.concat(list.stream(), Stream.of(pRhs)).toList()
            );
        } else {
            throw new PeelException(
                    "Can't add Map and Primitive value"
            );
        }
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
                        int occurrences = 0;
                        while ((stringLhs).contains(stringRhs)) {
                            occurrences++;
                            stringLhs = stringLhs.replaceFirst(stringRhs, "");
                        }
                        return new de.flogehring.peel.core.values.Number.Integer(occurrences);
                    } else {
                        throw new NoFunctionFoundException(
                                "Can't count types {0} {1}",
                                lhs.getClass().getName(),
                                rhs.getClass().getName()
                        );
                    }
                }
        );
    }
}
