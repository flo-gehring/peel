package de.flogehring.peel.parse;

import de.flogehring.peel.core.TypeDescriptor;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Program;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.flogehring.peel.core.lang.CodeElement.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PeelGrammarTest {

    @Test
    void simple() {
        Program parse = PeelGrammar.parse("""
                 a = 1 + 2
                 b = 5 + 2
                 a + b
                """);
        assertThat(parse).isEqualTo(
                new Program(
                        List.of(
                                assign("a", expr(getNumberLiteral(1), "+", getNumberLiteral(2))),
                                assign("b", expr(getNumberLiteral(5), "+", getNumberLiteral(2))),
                                expr(var("a"), "+", var("b"))
                        )
                )
        );
    }

    private static Expression.Literal getNumberLiteral(int literal) {
        return new Expression.Literal(TypeDescriptor.type(Number.class), literal);
    }
}
