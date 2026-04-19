package de.flogehring.peel.core.trace;

import java.util.List;

public sealed interface TraceValue permits
        TraceValue.IntegerValue,
        TraceValue.FloatValue,
        TraceValue.DecimalValue,
        TraceValue.TextValue,
        TraceValue.BoolValue,
        TraceValue.NoneValue,
        TraceValue.ListValue,
        TraceValue.MapValue,
        TraceValue.CallableRef {

    TraceValue.NoneValue NONE_VALUE = new NoneValue();

    static NoneValue none() {
        return TraceValue.NONE_VALUE;
    }

    static IntegerValue integer(int i) {
        return new IntegerValue(i);
    }

    static BoolValue bool(boolean b) {
        return new BoolValue(b);
    }

    static TextValue text(String x) {
        return new TextValue(x);
    }

    record IntegerValue(int value) implements TraceValue {
    }

    record FloatValue(float value) implements TraceValue {
    }

    record DecimalValue(String value) implements TraceValue {
    }

    record TextValue(String value) implements TraceValue {
    }

    record BoolValue(boolean value) implements TraceValue {
    }

    record NoneValue() implements TraceValue {
    }

    record ListValue(List<TraceValue> items) implements TraceValue {
    }

    record MapValue(List<MapEntry> entries) implements TraceValue {
        public record MapEntry(TraceValue key, TraceValue value) {
        }
    }

    record CallableRef(String callableKind, String name, List<String> arities) implements TraceValue {
    }
}
