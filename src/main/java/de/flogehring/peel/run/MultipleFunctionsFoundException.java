package de.flogehring.peel.run;

import java.text.MessageFormat;

public class MultipleFunctionsFoundException extends PeelException {
    public MultipleFunctionsFoundException(String functionName, int numberOfMatchingFound) {
        super(MessageFormat.format(
                "Expected one matching function with name {0}, found {1}",
                functionName, numberOfMatchingFound
        ));
    }
}
