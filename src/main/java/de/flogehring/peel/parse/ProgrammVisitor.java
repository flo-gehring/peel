package de.flogehring.peel.parse;

import de.flogehring.peel.antlr.PeelParser;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.Expression.IfElseStatement.ConditionalExecution;
import de.flogehring.peel.core.lang.ExpressionFactoryMethods;
import de.flogehring.peel.core.values.Bool;
import de.flogehring.peel.core.values.None;
import de.flogehring.peel.core.values.PeelCallable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;

class ProgrammVisitor implements de.flogehring.peel.antlr.PeelVisitor<ParsableProgramm> {

    private final List<Scope> scopes;
    private int functionDepth = 0;

    ProgrammVisitor() {
        this.scopes = new ArrayList<>();
    }

    private static class VariableDeclaration {
        String varName;
        boolean initialized;
        boolean isFunction;

        public VariableDeclaration(String varName, boolean isInitialized) {
            this.varName = varName;
            this.initialized = isInitialized;
            this.isFunction = false;
        }

        public VariableDeclaration(String varName, boolean isInitialized, boolean isFunction) {
            this.varName = varName;
            this.initialized = isInitialized;
            this.isFunction = isFunction;
        }

        String s() {
            return varName;
        }

        boolean initialized() {
            return initialized;
        }

    }

    record Scope(Set<VariableDeclaration> content) {

        static Scope empty() {
            return new Scope(new HashSet<>());
        }

        void addVar(String s) {
            if (inScope(s)) {
                throw new PeelParsingException("Identifier " + s + " already declared in scope");
            }
            content.add(new VariableDeclaration(s, false));
        }

        void addFunction(String s) {
            boolean inScope = inScope(s);
            if (inScope && !isFunction(s)) {
                throw new RedeclaredVariableException("Can't  redeclare variable " + s + " as function");
            }
            if (!inScope) {
                content.add(new VariableDeclaration(s, true, true));
            }
        }

        private boolean isFunction(String var) {
            return content.stream().anyMatch(
                    declaration -> declaration.s().equals(var) &&
                            declaration.isFunction
            );
        }

        boolean inScope(String s) {
            return content.stream().anyMatch(
                    var -> var.s().equals(s)
            );
        }

        boolean isInitialized(String var) {
            return content.stream().anyMatch(
                    declaration -> declaration.s().equals(var) &&
                            declaration.initialized()
            );
        }

        void setInitialized(String s) {
            if (!inScope(s)) {
                throw new PeelParsingException(
                        "Can't initialize Variable " + s + ", not in Scope"
                );
            }
            get(s).initialized = true;
        }

        private VariableDeclaration get(String name) {
            return content.stream().filter(
                    declaration -> declaration.s().equals(name)
            ).findAny().orElseThrow();
        }

    }


    private void beginScope() {
        scopes.add(Scope.empty());
    }

    private void endScope() {
        scopes.removeLast();
    }

    @Override
    public ParsableProgramm visitFunctionDeclaration(PeelParser.FunctionDeclarationContext ctx) {
        String name = ctx.IDENT().getText();
        scopes.getLast().addFunction(name);
        beginScope();
        List<String> parameters = ((ParsableProgramm.ParsableParameters) visitParameters(ctx.parameters())).parameters();
        parameters.forEach(this::initVar);
        parameters.forEach(this::setInitialized);
        functionDepth++;
        Expression.Block expr = (Expression.Block) visitBlock(ctx.block()).toExpr();
        functionDepth--;
        endScope();
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.FunctionDeclaration(
                        PeelCallable.userDefinedFunction(
                                name,
                                parameters,
                                expr
                        ),
                        0
                )
        );
    }

    @Override
    public ParsableProgramm visitParameters(PeelParser.ParametersContext ctx) {
        return new ParsableProgramm.ParsableParameters(ctx.IDENT().stream().map(TerminalNode::getText).toList());
    }

    @Override
    public ParsableProgramm visitFunctionCallExpr(PeelParser.FunctionCallExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.FunctionCall(
                        ctx.expr().accept(this).toExpr(),
                        visitArguments(ctx.arguments()).arguments()
                )
        );
    }

    @Override
    public ParsableProgramm visitLambdaExpr(PeelParser.LambdaExprContext ctx) {
        beginScope();
        List<String> parameters = ((ParsableProgramm.ParsableParameters) visitParameters(ctx.parameters())).parameters();
        parameters.forEach(this::initVar);
        parameters.forEach(this::setInitialized);
        functionDepth++;
        Expression.Block expr = (Expression.Block) visitBlock(ctx.block()).toExpr();
        functionDepth--;
        endScope();
        Token start = ctx.getStart();
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.Literal(
                        PeelCallable.userDefinedFunction(
                                "<anonymous_function>@" + start.getLine() + ":" + start.getCharPositionInLine(),
                                parameters,
                                expr
                        )
                )
        );
    }

    @Override
    public ParsableProgramm.ParsableArguments visitArguments(PeelParser.ArgumentsContext ctx) {
        return new ParsableProgramm.ParsableArguments(
                ctx.expr()
                        .stream().map(exprContext -> exprContext.accept(this))
                        .map(ParsableProgramm::toExpr).toList()
        );
    }

    @Override
    public ParsableProgramm visitProgram(de.flogehring.peel.antlr.PeelParser.ProgramContext ctx) {
        beginScope();
        List<ParsableProgramm> programm = ctx.statement()
                .stream()
                .map(child -> child.accept(this))
                .toList();
        endScope();
        return new ParsableProgramm.Programm(programm);
    }

    @Override
    public ParsableProgramm visitStatement(de.flogehring.peel.antlr.PeelParser.StatementContext ctx) {
        if (ctx.assignment() != null) {
            ParseTree child = assertOneChild(ctx);
            return child.accept(this);
        } else if (ctx.declaration() != null) {
            return ctx.declaration().accept(this);
        } else if (ctx.ifStatement() != null) {
            return ctx.ifStatement().accept(this);
        } else if (ctx.whileStatement() != null) {
            return ctx.whileStatement().accept(this);
        } else if (ctx.forEachStatement() != null) {
            return ctx.forEachStatement().accept(this);
        } else if (ctx.functionDeclaration() != null) {
            return ctx.functionDeclaration().accept(this);
        } else if (ctx.returnStatement() != null) {
            return ctx.returnStatement().accept(this);
        } else {
            return ctx.expr().accept(this);
        }
    }

    @Override
    public ParsableProgramm visitDeclaration(de.flogehring.peel.antlr.PeelParser.DeclarationContext ctx) {
        String varName = ctx.IDENT().getSymbol().getText();
        if (alreadyInScope(varName)) {
            throw new RedeclaredVariableException(
                    "Can't redeclare variable in same scope"
            );
        }
        initVar(varName);
        Expression exp = ctx.expr().accept(this).toExpr();
        setInitialized(varName);
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.Assignment(
                        varName,
                        exp,
                        0
                )
        );
    }

    private boolean alreadyInScope(String varName) {
        return scopes.getLast().inScope(varName);
    }

    private void setInitialized(String varName) {
        scopes.getLast().setInitialized(varName);
    }

    private void initVar(String varName) {
        scopes.getLast().addVar(varName);
    }

    private ParseTree assertOneChild(ParserRuleContext ctx) {
        assert ctx.getChildCount() == 1;
        return ctx.getChild(0);
    }

    @Override
    public ParsableProgramm visitAssignment(de.flogehring.peel.antlr.PeelParser.AssignmentContext ctx) {
        String varName = ctx.IDENT().getSymbol().getText();
        ParsableProgramm accept = ctx.expr().accept(this);
        int scopeOffset = findVar(varName);
        if (scopeOffset == -1) {
            throw new AssignmentToUndeclaredVariable(
                    "Can't assign to undeclared Variable " + varName
            );
        }
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.assign(
                        varName,
                        accept.toExpr(),
                        scopeOffset
                )
        );
    }

    private int findVar(String text) {
        int offset = 0;
        for (int i = scopes.size() - 1; i >= 0; --i) {
            Scope scope = scopes.get(i);
            if (scope.inScope(text)) {
                return offset;
            }
            offset += 1;
        }
        return -1;
    }

    @Override
    public ParsableProgramm visitTernaryExpr(de.flogehring.peel.antlr.PeelParser.TernaryExprContext ctx) {
        beginScope();
        Expression ifExpr = ctx.getChild(0).accept(this).toExpr();
        endScope();
        Expression thenExpr = ctx.getChild(2).accept(this).toExpr();
        Expression elseExpr = ctx.getChild(4).accept(this).toExpr();
        Expression.IfElseStatement codeElement = new Expression.IfElseStatement(
                List.of(
                        new ConditionalExecution(
                                ifExpr,
                                thenExpr
                        )
                ),
                Optional.of(elseExpr)
        );
        return new ParsableProgramm.ParsableCodeElement(
                codeElement
        );
    }

    @Override
    public ParsableProgramm visitWhileStatement(de.flogehring.peel.antlr.PeelParser.WhileStatementContext ctx) {
        beginScope();
        Expression loopCondition = ctx.expr().accept(this).toExpr();
        Expression.Block block = (Expression.Block) ctx.block().accept(this).toExpr();
        endScope();
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.WhileLoop(loopCondition, block)
        );
    }

    @Override
    public ParsableProgramm visitForEachStatement(de.flogehring.peel.antlr.PeelParser.ForEachStatementContext ctx) {
        Expression expr = ctx.expr().accept(this).toExpr();
        beginScope();
        String varName = ctx.getChild(2).getText();
        initVar(varName);
        setInitialized(varName);
        Expression.Block body = (Expression.Block) ctx.block().accept(this).toExpr();
        endScope();
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.ForEachLoop(
                        varName,
                        expr,
                        body
                )
        );
    }

    @Override
    public ParsableProgramm visitLogicalOrExpr(de.flogehring.peel.antlr.PeelParser.LogicalOrExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.expr(0).accept(this).toExpr(),
                        "||",
                        ctx.expr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitIfStatement(de.flogehring.peel.antlr.PeelParser.IfStatementContext ctx) {
        var blocks = ctx.block();
        var ifClauses = ctx.expr();
        List<ConditionalExecution> conditionals = new ArrayList<>();
        for (int i = 0; i < ifClauses.size(); ++i) {
            beginScope();
            Expression ifClause = ifClauses.get(i).accept(this).toExpr();
            endScope();
            Expression thenExpr = blocks.get(i).accept(this).toExpr();
            conditionals.add(
                    new ConditionalExecution(
                            ifClause,
                            thenExpr
                    )
            );
        }
        Optional<Expression> elseBlock = Optional.empty();
        if (blocks.size() > ifClauses.size()) {
            elseBlock = Optional.of(
                    blocks.getLast().accept(this).toExpr()
            );
        }
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.IfElseStatement(
                        conditionals,
                        elseBlock
                )
        );
    }

    @Override
    public ParsableProgramm visitReturnStatement(
            PeelParser.ReturnStatementContext ctx
    ) {
        if (functionDepth == 0) {
            Token start = ctx.start;

            throw new PeelParsingException(
                    "Error at return Statement, outside of function definition: Line " +
                            getPosString(start)
            );
        }
        return new ParsableProgramm.ParsableCodeElement(new Expression.Return(
                ctx.expr() != null ? ctx.expr().accept(this).toExpr() : new Expression.Literal(None.NONE)
        ));
    }

    private static String getPosString(Token token) {
        return token.getLine() + ":" + token.getCharPositionInLine();
    }

    private static String unescapeStringLiteral(String raw) {
        String quoted = raw.substring(1, raw.length() - 1);
        StringBuilder sb = new StringBuilder(quoted.length());
        for (int i = 0; i < quoted.length(); i++) {
            char c = quoted.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (i + 1 >= quoted.length()) {
                throw new PeelParsingException("Invalid string literal escape at end of token");
            }
            char esc = quoted.charAt(++i);
            switch (esc) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                default -> throw new PeelParsingException("Unsupported escape sequence: \\" + esc);
            }
        }
        return sb.toString();
    }


    @Override
    public ParsableProgramm visitBlock(de.flogehring.peel.antlr.PeelParser.BlockContext ctx) {
        beginScope();
        List<Expression> expressions = ctx.statement()
                .stream()
                .map(stmt -> stmt.accept(this).toExpr())
                .toList();
        endScope();
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.Block(expressions)
        );
    }

    @Override
    public ParsableProgramm visitIfExpr(de.flogehring.peel.antlr.PeelParser.IfExprContext ctx) {
        var ifClauses = ctx.expr();
        var thenBlocks = ctx.block();
        List<ConditionalExecution> conditionals = new ArrayList<>();
        for (int i = 0; i < ifClauses.size(); ++i) {
            beginScope();
            Expression ifClause = ifClauses.get(i).accept(this).toExpr();
            endScope();
            Expression thenBlock = thenBlocks.get(i).accept(this).toExpr();
            conditionals.add(
                    new ConditionalExecution(
                            ifClause,
                            thenBlock
                    )
            );
        }
        Optional<Expression> elseBlock = Optional.empty();
        if (thenBlocks.size() > ifClauses.size()) {
            elseBlock = Optional.of(
                    thenBlocks.getLast().accept(this).toExpr()
            );
        }
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.IfElseStatement(
                        conditionals,
                        elseBlock
                )
        );
    }

    @Override
    public ParsableProgramm visitVarExpr(de.flogehring.peel.antlr.PeelParser.VarExprContext ctx) {
        String varName = ctx.getText();
        int scopeOffset = checkIfVarIsInitialized(varName);
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.var(varName, scopeOffset)
        );
    }

    private int checkIfVarIsInitialized(String varName) {
        int scopeOffset = findVar(varName);
        if (scopeOffset != -1) {
            Scope scope = scopes.get(scopes.size() - 1 - scopeOffset);
            if (!scope.isInitialized(varName)) {
                throw new UninitializedVarExpression(
                        "Can't access uninitialized variable " + varName
                );
            }
        }
        return scopeOffset;
    }

    @Override
    public ParsableProgramm visitEqExpr(de.flogehring.peel.antlr.PeelParser.EqExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.expr(0).accept(this).toExpr(),
                        ctx.getChild(1).getText(),
                        ctx.expr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNotExpr(de.flogehring.peel.antlr.PeelParser.NotExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.UnaryPrefixOperator(
                        "!",
                        ctx.expr().accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitAddSubExpr(de.flogehring.peel.antlr.PeelParser.AddSubExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.expr(0).accept(this).toExpr(),
                        ctx.getChild(1).getText(),
                        ctx.expr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitLogicalAndExpr(de.flogehring.peel.antlr.PeelParser.LogicalAndExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.expr(0).accept(this).toExpr(),
                        "&&",
                        ctx.expr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNumberExpr(de.flogehring.peel.antlr.PeelParser.NumberExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.integer(Integer.valueOf(ctx.INTEGER().getText()))
        );
    }

    @Override
    public ParsableProgramm visitDecimalExpr(PeelParser.DecimalExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.decimal(new BigDecimal(ctx.DECIMAL().getText()))
        );
    }

    @Override
    public ParsableProgramm visitTrueExpr(PeelParser.TrueExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.Literal(new Bool(true))
        );
    }

    @Override
    public ParsableProgramm visitFalseExpr(PeelParser.FalseExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.Literal(new Bool(false))
        );
    }

    @Override
    public ParsableProgramm visitStringExpr(PeelParser.StringExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.string(unescapeStringLiteral(ctx.STRING().getText()))
        );
    }

    @Override
    public ParsableProgramm visitXorExpr(de.flogehring.peel.antlr.PeelParser.XorExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.expr(0).accept(this).toExpr(),
                        "^",
                        ctx.expr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitMulDivExpr(de.flogehring.peel.antlr.PeelParser.MulDivExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.expr(0).accept(this).toExpr(),
                        ctx.getChild(1).getText(),
                        ctx.expr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitPowExpr(PeelParser.PowExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.expr(0).accept(this).toExpr(),
                        "**",
                        ctx.expr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNegateExpr(PeelParser.NegateExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.UnaryPrefixOperator(
                        "-",
                        ctx.expr().accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visit(ParseTree tree) {
        int childCount1 = tree.getChildCount();
        List<ParsableProgramm> steps = new ArrayList<>();
        beginScope();
        for (int i = 0; i < childCount1 - 1; ++i) {
            steps.add(
                    tree.getChild(i).accept(this)
            );
        }
        endScope();
        return new ParsableProgramm.Programm(steps);
    }

    @Override
    public ParsableProgramm visitChildren(RuleNode node) {
        int childCount = node.getChildCount();
        return node.getChild(childCount - 1).accept(this);
    }

    @Override
    public ParsableProgramm visitListExpr(de.flogehring.peel.antlr.PeelParser.ListExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.ListLiteral(
                        ctx.expr().stream().map(exprCtx -> exprCtx.accept(this).toExpr()).toList()

                )
        );
    }

    @Override
    public ParsableProgramm visitMapExpr(PeelParser.MapExprContext ctx) {
        List<Expression.MapLiteral.Entry> entries = new ArrayList<>();
        List<PeelParser.ExprContext> expressions = ctx.expr();
        for (int i = 0; i + 1 < expressions.size(); i += 2) {
            entries.add(
                    new Expression.MapLiteral.Entry(
                            expressions.get(i).accept(this).toExpr(),
                            expressions.get(i + 1).accept(this).toExpr()
                    )
            );
        }
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.MapLiteral(entries)
        );
    }

    @Override
    public ParsableProgramm visitSelectorExpr(PeelParser.SelectorExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.Selector(
                        ctx.expr(0).accept(this).toExpr(),
                        ctx.expr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitTerminal(TerminalNode node) {
        return node.getChild(0).accept(this);
    }

    @Override
    public ParsableProgramm visitErrorNode(ErrorNode node) {
        throw new PeelParsingException(MessageFormat.format(
                "Parsing error on {0}:{1} -> {2}",
                node.getSourceInterval().a,
                node.getSourceInterval().b,
                node.getText()
        ));
    }

    @Override
    public ParsableProgramm visitParenExpr(de.flogehring.peel.antlr.PeelParser.ParenExprContext ctx) {
        return ctx.expr().accept(this);
    }
}
