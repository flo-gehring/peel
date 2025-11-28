package de.flogehring.peel.core.values;

public sealed interface Primitives extends PeelValue permits
        Number,
        Text,
        Bool {
}