package de.flogehring.peel.eval;

public sealed interface TypeDescriptor {

    static <T> TypeDescriptor type(Class<T> type) {
        return new Type<>(type);
    }
    record Type<T>(Class<T> type) implements TypeDescriptor {

    }

    record ListOf<T>(Class<T> type) implements TypeDescriptor {

    }
}
