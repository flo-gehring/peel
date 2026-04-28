package de.flogehring.peel.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Nullable {
}
