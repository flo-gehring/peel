package de.flogehring.peel.core.trace;

import de.flogehring.peel.core.values.*;

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
            case FunctionReference functionReference ->
                    new TraceValue.CallableRef(CallableKind.FUNCTION_REFERENCE, functionReference.getName(), List.of());
            case PeelClosure closure -> new TraceValue.CallableRef(
                    CallableKind.CLOSURE,
                    closure.getName(),
                    closure.getParameters()
            );
            case PeelFunctionDefinition functionDefinition -> new TraceValue.CallableRef(
                    CallableKind.PEEL_FUNCTION,
                    functionDefinition.getName(),
                    functionDefinition.getParameters()
            );
        };
    }

    private static TraceValue.MapValue.MapEntry fromMapEntry(Map.Entry<Primitives, PeelValue> entry) {
        return new TraceValue.MapValue.MapEntry(
                fromPeelValue(entry.getKey()),
                fromPeelValue(entry.getValue())
        );
    }
}
