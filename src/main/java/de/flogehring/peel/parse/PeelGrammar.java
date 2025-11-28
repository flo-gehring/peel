package de.flogehring.peel.parse;

import de.flogehring.peel.antlr.PeelLexer;
import de.flogehring.peel.antlr.PeelParser;
import de.flogehring.peel.core.lang.CodeElement;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.run.PeelException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class PeelGrammar {

    private static final String GRAMMAR_DEFINITION = """
            Program  <- CodeElements
            CodeElements <- CodeElement+
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
        PeelLexer peelLexer = new PeelLexer(CharStreams.fromString(program));
        CommonTokenStream tokens = new CommonTokenStream(peelLexer);
        PeelParser parser = new PeelParser(tokens);
        PeelParser.ProgramContext tree = parser.program();
        ProgrammVisitor visitor = new ProgrammVisitor();
        ParsableProgramm visit = visitor.visit(tree);
        if (visit instanceof ParsableProgramm.Programm programm) {
            return programm.toProgramm();
        } else {
            throw new PeelException("Parsing gone wrong");
        }
    }

    static class ProgrammVisitor implements de.flogehring.peel.antlr.PeelVisitor<ParsableProgramm> {

        @Override
        public ParsableProgramm visitProgram(PeelParser.ProgramContext ctx) {
            return new ParsableProgramm.Programm(
                    ctx.statement()
                            .stream()
                            .map(child -> child.accept(this))
                            .toList()
            );
        }

        @Override
        public ParsableProgramm visitStatement(PeelParser.StatementContext ctx) {
            if (ctx.assignment() != null) {
                ParseTree child = assertOneChild(ctx);
                return child.accept(this);
            } else {
                return ctx.expr().accept(this);
            }
        }

        private ParseTree assertOneChild(ParserRuleContext ctx) {
            assert ctx.getChildCount() == 1;
            return ctx.getChild(0);
        }

        @Override
        public ParsableProgramm visitAssignment(PeelParser.AssignmentContext ctx) {
            String text = ctx.IDENT().getSymbol().getText();
            ParsableProgramm accept = ctx.expr().accept(this);
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.assign(
                            text,
                            accept.toExpr()
                    )
            );
        }

        @Override
        public ParsableProgramm visitLogicalOrExpr(PeelParser.LogicalOrExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.expr(
                            ctx.expr(0).accept(this).toExpr(),
                            "||",
                            ctx.expr(1).accept(this).toExpr()
                    )
            );
        }

        @Override
        public ParsableProgramm visitVarExpr(PeelParser.VarExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.var(ctx.getText())
            );
        }

        @Override
        public ParsableProgramm visitEqExpr(PeelParser.EqExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.expr(
                            ctx.expr(0).accept(this).toExpr(),
                            "==",
                            ctx.expr(1).accept(this).toExpr()
                    )
            );
        }

        @Override
        public ParsableProgramm visitNotExpr(PeelParser.NotExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    ctx.accept(this).toExpr() // TODO not operator
            );
        }

        @Override
        public ParsableProgramm visitAddSubExpr(PeelParser.AddSubExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.expr(
                            ctx.expr(0).accept(this).toExpr(),
                            ctx.getChild(1).getText(),
                            ctx.expr(1).accept(this).toExpr()
                    )
            );
        }

        @Override
        public ParsableProgramm visitLogicalAndExpr(PeelParser.LogicalAndExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.expr(
                            ctx.expr(0).accept(this).toExpr(),
                            "&&",
                            ctx.expr(1).accept(this).toExpr()
                    )
            );
        }

        @Override
        public ParsableProgramm visitNumberExpr(PeelParser.NumberExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.integer(Integer.valueOf(ctx.NUMBER().getText()))
            );
        }

        @Override
        public ParsableProgramm visitXorExpr(PeelParser.XorExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.expr(
                            ctx.expr(0).accept(this).toExpr(),
                            "^",
                            ctx.expr(1).accept(this).toExpr()
                    )
            );
        }

        @Override
        public ParsableProgramm visitMulDivExpr(PeelParser.MulDivExprContext ctx) {
            return new ParsableProgramm.ParsableCodeElement(
                    CodeElement.expr(
                            ctx.expr(0).accept(this).toExpr(),
                            ctx.getChild(1).getText(),
                            ctx.expr(1).accept(this).toExpr()
                    )
            );
        }

        @Override
        public ParsableProgramm visit(ParseTree tree) {
            int childCount1 = tree.getChildCount();
            List<ParsableProgramm> steps = new ArrayList<>();
            for (int i = 0; i < childCount1 - 1; ++i) {
                steps.add(
                        tree.getChild(i).accept(this)
                );
            }

            return new ParsableProgramm.Programm(steps);
        }

        @Override
        public ParsableProgramm visitChildren(RuleNode node) {
            int childCount = node.getChildCount();
            return node.getChild(childCount - 1).accept(this);
        }

        @Override
        public ParsableProgramm visitTerminal(TerminalNode node) {
            return node.getChild(0).accept(this);
        }

        @Override
        public ParsableProgramm visitErrorNode(ErrorNode node) {
            throw new PeelException(MessageFormat.format(
                    "Parsing error on {0}:{1} -> {2}",
                    node.getSourceInterval().a,
                    node.getSourceInterval().b,
                    node.getText()
            ));
        }

        @Override
        public ParsableProgramm visitParenExpr(PeelParser.ParenExprContext ctx) {
            return visit(ctx);
        }
    }
}
