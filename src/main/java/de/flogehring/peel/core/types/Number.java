package de.flogehring.peel.core.types;

public sealed interface Number extends Primitives {

    record Integer() implements Number {

    }

    record Float() implements Number {

    }

    record Decimal() implements Number {

    }

    record Complex() implements Number {

    }
}
