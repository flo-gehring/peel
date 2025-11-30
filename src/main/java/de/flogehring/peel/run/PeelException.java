package de.flogehring.peel.run;

import java.text.MessageFormat;

public class PeelException extends RuntimeException {
    public PeelException(String message) {
        super(message);
    }

    public PeelException(String message, Object... args) {
        super(MessageFormat.format(message, args));
    }
}
