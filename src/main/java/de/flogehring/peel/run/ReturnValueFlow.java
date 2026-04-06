package de.flogehring.peel.run;

import de.flogehring.peel.core.values.PeelValue;
import lombok.Getter;

public class ReturnValueFlow extends RuntimeException {
    @Getter
    private final PeelValue value;

    public ReturnValueFlow(PeelValue value) {
        super("");
        this.value = value;
    }
}
