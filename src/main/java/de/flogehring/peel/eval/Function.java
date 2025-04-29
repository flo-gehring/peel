package de.flogehring.peel.eval;

import java.util.List;

public interface Function {

    String name();

    List<TypeDescriptor> arguments();

    Object run(Object... arguments);
}
