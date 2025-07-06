package de.flogehring.peel.core.values;

import de.flogehring.peel.core.types.PeelTypes;
import de.flogehring.peel.core.types.Primitives;

public sealed interface PeelValue {

    Object value();
    PeelTypes type();

    record Primitive(Primitives type, Object value) implements  PeelValue {

    }

    sealed interface Collection extends PeelValue {
        record Map(java.util.Map<Primitive, PeelValue> map) implements Collection {
            @Override
            public Object value() {
                return map;
            }

            @Override
            public PeelTypes type() {
                return new de.flogehring.peel.core.types.Map();
            }
        }

        record List(java.util.List<PeelValue> list) implements Collection {
            @Override
            public Object value() {
                return list;
            }

            @Override
            public PeelTypes type() {
                return new de.flogehring.peel.core.types.List();
            }
        }
    }
}
