package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.eval.RuntimeModule;
import de.flogehring.peel.core.values.*;
import de.flogehring.peel.run.ExecutionBudget;
import de.flogehring.peel.run.exceptions.NoFunctionFoundException;
import de.flogehring.peel.run.exceptions.PeelException;
import de.flogehring.peel.run.trace.FunctionCallRecorder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

final class StandardRuntimeModule {

    static final RuntimeModule INSTANCE = RuntimeModule.of(
            List.of(),
            List.of(countSubstring()),
            List.of(
                    OperatorDef.typedWithBudget(
                            "+",
                            PeelValue.Collection.class,
                            PeelValue.Collection.class,
                            (budget, lhs, rhs) -> addCollections(budget, (PeelValue.Collection) lhs, (PeelValue.Collection) rhs)
                    ),
                    OperatorDef.typedWithBudget(
                            "+",
                            PeelValue.Collection.class,
                            Primitives.class,
                            (budget, lhs, rhs) -> addCollectionAnd(budget, (PeelValue.Collection) lhs, (Primitives) rhs)
                    ),
                    OperatorDef.typedWithBudget(
                            "+",
                            PeelValue.Collection.class,
                            PeelCallable.class,
                            (budget, lhs, rhs) -> addCollectionAnd(budget, (PeelValue.Collection) lhs, (PeelCallable) rhs)
                    ),
                    OperatorDef.typedWithBudget(
                            "+",
                            Text.class,
                            Text.class,
                            (budget, lhs, rhs) -> addText(budget, (Text) lhs, (Text) rhs)
                    ),
                    OperatorDef.typedWithBudget(
                            "-",
                            PeelValue.Collection.class,
                            PeelValue.Collection.class,
                            (budget, lhs, rhs) -> subCollection(budget, (PeelValue.Collection) lhs, (PeelValue.Collection) rhs)
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
                            "!=",
                            PeelValue.class,
                            PeelValue.class,
                            (lhs, rhs) -> PeelValue.bool(!lhs.equals(rhs))
                    ),
                    OperatorDef.typed(
                            "^",
                            Bool.class,
                            Bool.class,
                            (lhs, rhs) -> PeelValue.bool(((Bool) lhs).value() ^ ((Bool) rhs).value())
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

    private static Text addText(ExecutionBudget budget, Text lhs, Text rhs) {
        budget.requireValueSize((long) lhs.value().length() + rhs.value().length());
        return new Text(lhs.value() + rhs.value());
    }

    private static PeelValue subCollection(ExecutionBudget budget, PeelValue.Collection cLhs, PeelValue.Collection cRhs) {
        return switch (cLhs) {
            case PeelValue.Collection.List(var listLhs) -> switch (cRhs) {
                case PeelValue.Collection.List(var listRhs) -> PeelValue.list(
                        listLhs.stream().filter(
                                valL -> listRhs.stream().noneMatch(valR -> {
                                    budget.step();
                                    return valL.equals(valR);
                                })
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
                            budget.step();
                            result.remove(key);
                        }
                        yield new PeelValue.Collection.Map(Collections.unmodifiableMap(result));
                    }
                }
            }
        };
    }

    private static <T extends PeelValue> PeelValue addCollectionAnd(ExecutionBudget budget, PeelValue.Collection cLhs, T pRhs) {
        if (cLhs instanceof PeelValue.Collection.List(List<PeelValue> list)) {
            budget.requireValueSize((long) list.size() + 1);
            return new PeelValue.Collection.List(
                    Stream.concat(list.stream(), Stream.of(pRhs)).toList()
            );
        } else {
            throw new PeelException(
                    "Can't add Map and Primitive value"
            );
        }
    }

    private static PeelValue addCollections(ExecutionBudget budget, PeelValue.Collection cLhs, PeelValue.Collection cRhs) {
        return switch (cLhs) {
            case PeelValue.Collection.List(var listLhs) -> switch (cRhs) {
                case PeelValue.Collection.List(var listRhs) -> {
                    budget.requireValueSize((long) listLhs.size() + listRhs.size());
                    yield PeelValue.list(Stream.concat(listLhs.stream(), listRhs.stream()).toList());
                }
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
                        for (var entry : mapRhs.entrySet()) {
                            budget.step();
                            if (!result.containsKey(entry.getKey())) {
                                budget.requireValueSize((long) result.size() + 1);
                            }
                            result.put(entry.getKey(), entry.getValue());
                        }
                        yield new PeelValue.Collection.Map(Collections.unmodifiableMap(result));
                    }
                }
            }
        };
    }

    private static Function countSubstring() {
        return new Function() {
            @Override
            public String name() {
                return "count";
            }

            @Override
            public int arity() {
                return 2;
            }

            @Override
            public PeelValue runWithTrace(FunctionCallRecorder recorder, PeelValue... arguments) {
                return runWithTrace(ExecutionBudget.unlimited(), recorder, arguments);
            }

            @Override
            public PeelValue runWithTrace(ExecutionBudget budget, FunctionCallRecorder recorder, PeelValue... arguments) {
                return countSubstring(arguments[0], arguments[1], budget);
            }
        };
    }

    private static PeelValue countSubstring(PeelValue lhs, PeelValue rhs, ExecutionBudget budget) {
        if (lhs instanceof Text(var stringLhs) && rhs instanceof Text(var stringRhs)) {
            int occurrences = 0;
            while (stringLhs.contains(stringRhs)) {
                budget.step();
                occurrences++;
                stringLhs = stringLhs.replaceFirst(stringRhs, "");
            }
            return new de.flogehring.peel.core.values.Number.Integer(occurrences);
        }
        throw new NoFunctionFoundException(
                "Can't count types {0} {1}",
                lhs.getClass().getName(),
                rhs.getClass().getName()
        );
    }
}
