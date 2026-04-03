package de.flogehring.peel.core.values;

import java.util.List;

public sealed interface PeelValue permits Primitives, PeelValue.Collection, PeelCallable {

    static Collection.List list(List<PeelValue> peelValue) {
        return new Collection.List(peelValue);
    }

    static Number.Integer integer(int i) {
        return new Number.Integer(i);
    }

    static Text text(String s) {
        return new Text(s);
    }

    static PeelValue bool(boolean b) {
        return new Bool(b);
    }

    sealed interface Collection extends PeelValue {

        static List peelList(java.util.List<PeelValue> list) {
            return new List(list);
        }

        static Map peelMap(java.util.Map<Primitives, PeelValue> map) {
            return new Map(map);
        }

        record Map(java.util.Map<Primitives, PeelValue> map) implements Collection {
            @Override
            public String toString() {
                return "{" + String.join(", ", map.entrySet().stream().map(entry -> entry.getKey().toString() + ": " + entry.getValue().toString()) + "}");
            }
        }

        record List(java.util.List<PeelValue> list) implements Collection {
            @Override
            public String toString() {
                return "[" + String.join(", ", list.stream().map(PeelValue::toString).toList()) + "]";
            }
        }
    }
}