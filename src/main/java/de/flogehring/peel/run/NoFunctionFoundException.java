package de.flogehring.peel.run;

import java.util.Arrays;

import static java.text.MessageFormat.format;

public class NoFunctionFoundException extends PeelException {

    public NoFunctionFoundException(String name, Object... arguments) {
        super(getErrorMessage(name, arguments));
    }

    private static String getErrorMessage(String name, Object[] arguments) {
        String typDescription = String.join(
                ",",
                Arrays.stream(arguments)
                        .map(Object::getClass)
                        .map(Class::getTypeName)
                        .toList()
        );
        return format(
                "Could not find function with name {0} and type arguments matching {1}",
                name,
                typDescription
        );
    }
}
