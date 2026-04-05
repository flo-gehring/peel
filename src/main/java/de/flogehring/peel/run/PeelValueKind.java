package de.flogehring.peel.run;

import de.flogehring.peel.core.values.*;

enum PeelValueKind {
    VALUE,
    PRIMITIVE,
    NUMBER,
    INTEGER,
    FLOAT,
    DECIMAL,
    TEXT,
    BOOL,
    NONE,
    COLLECTION,
    LIST,
    MAP,
    CALLABLE;

    static PeelValueKind fromValue(PeelValue value) {
        return switch (value) {
            case de.flogehring.peel.core.values.Number.Integer _ -> INTEGER;
            case de.flogehring.peel.core.values.Number.Float _ -> FLOAT;
            case de.flogehring.peel.core.values.Number.Decimal _ -> DECIMAL;
            case Text _ -> TEXT;
            case Bool _ -> BOOL;
            case None _ -> NONE;
            case PeelValue.Collection.List _ -> LIST;
            case PeelValue.Collection.Map _ -> MAP;
            case PeelCallable _ -> CALLABLE;
        };
    }

    static PeelValueKind fromDeclaredType(Class<? extends PeelValue> type) {
        if (type.equals(PeelValue.class)) {
            return VALUE;
        }
        if (type.equals(Primitives.class)) {
            return PRIMITIVE;
        }
        if (type.equals(de.flogehring.peel.core.values.Number.class)) {
            return NUMBER;
        }
        if (type.equals(de.flogehring.peel.core.values.Number.Integer.class)) {
            return INTEGER;
        }
        if (type.equals(de.flogehring.peel.core.values.Number.Float.class)) {
            return FLOAT;
        }
        if (type.equals(de.flogehring.peel.core.values.Number.Decimal.class)) {
            return DECIMAL;
        }
        if (type.equals(Text.class)) {
            return TEXT;
        }
        if (type.equals(Bool.class)) {
            return BOOL;
        }
        if (type.equals(None.class)) {
            return NONE;
        }
        if (type.equals(PeelValue.Collection.class)) {
            return COLLECTION;
        }
        if (type.equals(PeelValue.Collection.List.class)) {
            return LIST;
        }
        if (type.equals(PeelValue.Collection.Map.class)) {
            return MAP;
        }
        if (type.equals(PeelCallable.class)) {
            return CALLABLE;
        }
        throw new IllegalArgumentException("Unsupported PeelValue type in operator signature: " + type.getName());
    }

    boolean matches(PeelValueKind actual) {
        if (this == actual) {
            return true;
        }
        return switch (this) {
            case VALUE -> true;
            case PRIMITIVE -> actual == NUMBER || actual == INTEGER || actual == FLOAT || actual == DECIMAL
                    || actual == TEXT || actual == BOOL || actual == NONE;
            case NUMBER -> actual == INTEGER || actual == FLOAT || actual == DECIMAL;
            case COLLECTION -> actual == LIST || actual == MAP;
            case INTEGER, FLOAT, DECIMAL, TEXT, BOOL, NONE, LIST, MAP, CALLABLE -> false;
        };
    }

    int distance(PeelValueKind actual) {
        if (!matches(actual)) {
            return Integer.MIN_VALUE;
        }
        if (this == actual) {
            return 10_000;
        }
        if (this == VALUE) {
            return 1;
        }
        if (this == PRIMITIVE || this == NUMBER || this == COLLECTION) {
            return 5_000;
        }
        return 0;
    }
}
