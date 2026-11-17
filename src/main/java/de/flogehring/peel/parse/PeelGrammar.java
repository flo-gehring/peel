package de.flogehring.peel.parse;

import de.flogehring.peel.antlr.PeelLexer;
import de.flogehring.peel.antlr.PeelParser;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.lang.BlockCatalog;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import java.util.List;

public class PeelGrammar {

    private PeelGrammar() {
    }

    public static Program parse(String program) {
        return parseWithBlocks(program).program();
    }

    public record ParsedProgram(Program program, BlockCatalog blockCatalog) {
        public List<String> blockNames() { return blockCatalog.names(); }
    }

    public static ParsedProgram parseWithBlocks(String program) {
        PeelLexer peelLexer = new PeelLexer(CharStreams.fromString(program));
        CommonTokenStream tokens = new CommonTokenStream(peelLexer);
        PeelParser parser = new PeelParser(tokens);
        PeelParser.ProgramContext tree = parser.program();
        ProgrammVisitor visitor = new ProgrammVisitor();
        ParsableProgramm visit = visitor.visit(tree);
        if (visit instanceof ParsableProgramm.Programm programm) {
            Program ast = programm.toProgramm();
            try {
                return new ParsedProgram(ast, BlockCatalog.analyze(ast));
            } catch (IllegalArgumentException error) {
                throw new PeelParsingException(error.getMessage());
            }
        } else {
            throw new PeelParsingException("Parsing gone wrong");
        }
    }
}
