package de.flogehring.peel.parse;

import de.friendlyhedgehog.jetpack.annotationmapper.Mapper;
import de.friendlyhedgehog.jetpack.datatypes.Node;
import de.friendlyhedgehog.jetpack.grammar.Symbol;
import de.friendlyhedgehog.jetpack.parse.Grammar;
import de.flogehring.peel.core.lang.Program;

public class PeelGrammar {

    private static final String GRAMMAR_DEFINITION = """
            Program  <- CodeElement+
            CodeElement <- Statement / Expression
            Statement <- Assignment
            Expression <- BinaryOperator / Literal / Variable
            Variable <- VariableName
            BinaryOperator <- Expression Operator Expression
            Literal <- Num
            Operator <- "\\+" / "\\*" / "[a-zA-Z]+"
            Assignment <- VariableName "=" Expression
            Num <- "[0-9]+"
            VariableName <- "[a-zA-Z]+"
            """;

    public static Program parse(String program) {
        Grammar peelGrammar = Grammar.of(GRAMMAR_DEFINITION).getEither();
        Node<Symbol> ast = peelGrammar.parse(program).getEither();
        try {
            ParsableProgram map = Mapper.defaultMapper()
                    .map(ast, ParsableProgram.class);
            return map.toProgramm();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
