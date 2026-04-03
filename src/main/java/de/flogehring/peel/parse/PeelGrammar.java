package de.flogehring.peel.parse;

import de.flogehring.peel.antlr.PeelLexer;
import de.flogehring.peel.antlr.PeelParser;
import de.flogehring.peel.core.lang.Program;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class PeelGrammar {

    private PeelGrammar() {
    }

    public static Program parse(String program) {
        PeelLexer peelLexer = new PeelLexer(CharStreams.fromString(program));
        CommonTokenStream tokens = new CommonTokenStream(peelLexer);
        PeelParser parser = new PeelParser(tokens);
        PeelParser.ProgramContext tree = parser.program();
        ProgrammVisitor visitor = new ProgrammVisitor();
        ParsableProgramm visit = visitor.visit(tree);
        if (visit instanceof ParsableProgramm.Programm programm) {
            return programm.toProgramm();
        } else {
            throw new PeelParsingException("Parsing gone wrong");
        }
    }
}
