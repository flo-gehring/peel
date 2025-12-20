package de.flogehring.peel.parse;

import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Number;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.flogehring.peel.core.lang.ExpressionFactoryMethods.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PeelGrammarTest {

    @Test
    void simple() {
        Program parse = PeelGrammar.parse("""
                 var a = 1 + 2;
                 var b = 5 + 2;
                 a + b;
                """);
        assertThat(parse).isEqualTo(
                new Program(
                        new Expression.Block(List.of(
                                assign("a", expr(getNumberLiteral(1), "+", getNumberLiteral(2)), 0),
                                assign("b", expr(getNumberLiteral(5), "+", getNumberLiteral(2)), 0),
                                expr(var("a", 0), "+", var("b", 0))
                        )
                        ))
        );
    }

    private static Expression.Literal getNumberLiteral(int literal) {
        return new Expression.Literal(new Number.Integer(literal));
    }
}
