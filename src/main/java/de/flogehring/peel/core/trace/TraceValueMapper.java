package de.flogehring.peel.core.trace;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.values.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TraceValueMapper {

    private TraceValueMapper() {
    }

    public static TraceValue fromPeelValue(PeelValue peelValue) {
        return switch (peelValue) {
            case de.flogehring.peel.core.values.Number.Integer(var value) -> new TraceValue.IntegerValue(value);
            case de.flogehring.peel.core.values.Number.Float(var value) -> new TraceValue.FloatValue(value);
            case de.flogehring.peel.core.values.Number.Decimal(var value) ->
                    new TraceValue.DecimalValue(value.toPlainString());
            case Text(var value) -> new TraceValue.TextValue(value);
            case Bool(var value) -> new TraceValue.BoolValue(value);
            case None _ -> TraceValue.none();
            case PeelValue.Collection.List(var list) -> new TraceValue.ListValue(
                    list.stream().map(TraceValueMapper::fromPeelValue).toList()
            );
            case PeelValue.Collection.Map(var map) -> new TraceValue.MapValue(
                    map.entrySet().stream().map(TraceValueMapper::fromMapEntry).toList()
            );
            case FunctionReference functionReference -> {
                String name = functionReference.getFunctions().isEmpty()
                        ? "<function_reference>"
                        : functionReference.getFunctions().getFirst().name();
                List<Integer> arities = functionReference.getFunctions().stream().map(Function::arity).toList();
                yield new TraceValue.CallableRef("function_reference", name, arities);
            }
            case PeelClosure closure -> new TraceValue.CallableRef(
                    "closure",
                    closure.getName(),
                    List.of(closure.getParameters().size())
            );
            case PeelFunctionDefinition functionDefinition -> new TraceValue.CallableRef(
                    "peel_function",
                    functionDefinition.getName(),
                    List.of(functionDefinition.getParameters().size())
            );
        };
    }

    public static PeelValue toPeelValue(TraceValue traceValue) {
        return switch (traceValue) {
            case TraceValue.IntegerValue(var value) -> PeelValue.integer(value);
            case TraceValue.FloatValue(var value) -> new de.flogehring.peel.core.values.Number.Float(value);
            case TraceValue.DecimalValue(var value) ->
                    new de.flogehring.peel.core.values.Number.Decimal(new java.math.BigDecimal(value));
            case TraceValue.TextValue(var value) -> PeelValue.text(value);
            case TraceValue.BoolValue(var value) -> PeelValue.bool(value);
            case TraceValue.NoneValue _ -> None.NONE;
            case TraceValue.ListValue(var items) ->
                    PeelValue.list(items.stream().map(TraceValueMapper::toPeelValue).toList());
            case TraceValue.MapValue(var entries) -> {
                LinkedHashMap<Primitives, PeelValue> map = new LinkedHashMap<>();
                for (TraceValue.MapValue.MapEntry entry : entries) {
                    PeelValue key = toPeelValue(entry.key());
                    if (key instanceof Primitives primitive) {
                        map.put(primitive, toPeelValue(entry.value()));
                    } else {
                        throw new IllegalArgumentException("Map key is not primitive trace value");
                    }
                }
                yield PeelValue.Collection.peelMap(Map.copyOf(map));
            }
            case TraceValue.CallableRef _ ->
                    throw new IllegalArgumentException("CallableRef cannot be mapped to runtime PeelValue");
        };
    }

    private static TraceValue.MapValue.MapEntry fromMapEntry(Map.Entry<Primitives, PeelValue> entry) {
        return new TraceValue.MapValue.MapEntry(
                fromPeelValue(entry.getKey()),
                fromPeelValue(entry.getValue())
        );
    }
}
