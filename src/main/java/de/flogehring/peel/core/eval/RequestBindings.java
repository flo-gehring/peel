package de.flogehring.peel.core.eval;

import de.flogehring.peel.core.values.PeelValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RequestBindings {

    private final Map<String, PeelValue> values;

    private RequestBindings(Map<String, PeelValue> values) {
        this.values = values;
    }

    public static RequestBindings empty() {
        return new RequestBindings(Map.of());
    }

    public static RequestBindings of(Variable... variables) {
        Builder builder = builder();
        for (Variable variable : variables) {
            builder.put(variable);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, PeelValue> values() {
        return values;
    }

    public static final class Builder {
        private final LinkedHashMap<String, PeelValue> values = new LinkedHashMap<>();

        public Builder put(String name, PeelValue value) {
            values.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
            return this;
        }

        public Builder put(Variable variable) {
            Objects.requireNonNull(variable, "variable");
            return put(variable.name(), variable.value());
        }

        public RequestBindings build() {
            return new RequestBindings(Map.copyOf(values));
        }
    }
}
