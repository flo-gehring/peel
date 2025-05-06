package de.flogehring.peel.run;

import de.flogehring.peel.eval.Function;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import static de.flogehring.peel.eval.TypeDescriptor.type;

public class FunctionFactory {

    private FunctionFactory() {
    }

    public static <S> Function unary(
            String name,
            Class<S> argument, UnaryOperator<S> f
    ) {
        return new SimpleFunction(
                name,
                List.of(type(argument)),
                type(argument),
                arguments -> f.apply(argument.cast(arguments[0]))
        );
    }

    public static <S, T> Function unary(
            String name,
            Class<S> argumentType, Class<T> returnType, java.util.function.Function<S, T> f
    ) {
        return new SimpleFunction(
                name,
                List.of(type(argumentType)),
                type(returnType),
                arguments -> f.apply(argumentType.cast(arguments[0]))
        );
    }

    public static <S, T, R> Function binary(
            String name,
            Class<S> firstArgType,
            Class<T> secondArgType,
            Class<R> returnType, BiFunction<S, T, R> f
    ) {
        return new SimpleFunction(
                name,
                List.of(type(firstArgType), type(secondArgType)),
                type(returnType),
                arguments -> f.apply(
                        firstArgType.cast(arguments[0].value()),
                        secondArgType.cast(arguments[1].value())
                )
        );
    }
}