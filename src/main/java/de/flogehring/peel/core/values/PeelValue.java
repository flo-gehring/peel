package de.flogehring.peel.core.values;

import java.util.List;

public sealed interface PeelValue permits Primitives, PeelValue.Collection {

    Object value();

    static Collection.List list(List<PeelValue> peelValue) {
        return new Collection.List(peelValue);
    }


    sealed interface Collection extends PeelValue {
        record Map(java.util.Map<Primitives, PeelValue> map) implements Collection {
            @Override
            public Object value() {
                return map;
            }

        }

        record List(java.util.List<PeelValue> list) implements Collection {
            @Override
            public Object value() {
                return list;
            }
        }
    }
}
