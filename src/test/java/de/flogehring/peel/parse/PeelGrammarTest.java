package de.flogehring.peel.parse;

import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.values.Number;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

    @Test
    void decimalLiteral() {
        Program parse = PeelGrammar.parse("100.00;");
        assertThat(parse).isEqualTo(
                new Program(
                        new Expression.Block(List.of(
                                decimal(new BigDecimal("100.00"))
                        ))
                )
        );
    }

    @Test
    void listSelectorExpression() {
        Program parse = PeelGrammar.parse("""
                var l = [1,2,3];
                l[1] + l[2];
                """);

        assertThat(parse).isEqualTo(
                new Program(
                        new Expression.Block(List.of(
                                assign("l", new Expression.ListLiteral(List.of(
                                        getNumberLiteral(1),
                                        getNumberLiteral(2),
                                        getNumberLiteral(3)
                                )), 0),
                                expr(
                                        new Expression.Selector(var("l", 0), getNumberLiteral(1)),
                                        "+",
                                        new Expression.Selector(var("l", 0), getNumberLiteral(2))
                                )
                        ))
                )
        );
    }

    @Test
    void stringLiteral() {
        Program parse = PeelGrammar.parse("\"hello\";");
        assertThat(parse).isEqualTo(
                new Program(
                        new Expression.Block(List.of(
                                string("hello")
                        ))
                )
        );
    }

    @Test
    void stringLiteralWithEscapes() {
        Program parse = PeelGrammar.parse("\"a\\n\\t\\\"b\\\\\";");
        assertThat(parse).isEqualTo(
                new Program(
                        new Expression.Block(List.of(
                                string("a\n\t\"b\\")
                        ))
                )
        );
    }
}
