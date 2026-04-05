package de.flogehring.peel.run.exceptions;

import de.flogehring.peel.core.eval.OperatorDef;

import java.util.List;
import java.util.stream.Collectors;

public class AmbiguousOperatorException extends PeelException {

    public AmbiguousOperatorException(String symbol, Object argument, List<OperatorDef> matches) {
        super(
                "Ambiguous operator ''{0}'' for argument type ({1}). Matching signatures: {2}",
                symbol,
                argument.getClass().getSimpleName(),
                matches.stream()
                        .map(OperatorDef::signature)
                        .collect(Collectors.joining(", "))
        );
    }

    public AmbiguousOperatorException(String symbol, Object lhs, Object rhs, List<OperatorDef> matches) {
        super(
                "Ambiguous operator ''{0}'' for argument types ({1}, {2}). Matching signatures: {3}",
                symbol,
                lhs.getClass().getSimpleName(),
                rhs.getClass().getSimpleName(),
                matches.stream()
                        .map(OperatorDef::signature)
                        .collect(Collectors.joining(", "))
        );
    }
}
