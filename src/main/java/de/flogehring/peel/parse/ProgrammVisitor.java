package de.flogehring.peel.parse;

import de.flogehring.peel.antlr.PeelParser;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.lang.ExpressionFactoryMethods;
import de.flogehring.peel.core.values.PeelCallable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.text.MessageFormat;
import java.util.*;

class ProgrammVisitor implements de.flogehring.peel.antlr.PeelVisitor<ParsableProgramm> {

    private final List<Scope> scopes;

    ProgrammVisitor() {
        this.scopes = new ArrayList<>();
    }

    private static class VariableDeclaration {
        String s;
        boolean initialized;

        public VariableDeclaration(String s, boolean b) {
            this.s = s;
            this.initialized = b;
        }

        String s() {
            return s;
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
        if (alreadyInScope(name)) { // TODO this is not right, because it allows for only one function definition per name
            throw new RedeclaredVariableException("Can't redeclare function with same scope");
        }
        initVar(name);
        setInitialized(name);
        beginScope();
        List<String> parameters = ((ParsableProgramm.ParsableParameters) visitParameters(ctx.parameters())).parameters();
        parameters.forEach(this::initVar);
        parameters.forEach(this::setInitialized);
        Expression.Block expr = (Expression.Block) visitBlock(ctx.block()).toExpr();
        endScope();
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.Assignment(
                        name,
                        new Expression.Literal(
                                PeelCallable.userDefinedFunction(
                                        name,
                                        parameters,
                                        expr
                                )),
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
        Expression.Block expr = (Expression.Block) visitBlock(ctx.block()).toExpr();
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
    public ParsableProgramm visitNonTernaryfunctionCallExpr(PeelParser.NonTernaryfunctionCallExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.FunctionCall(
                        ctx.nonTernaryExpr().accept(this).toExpr(),
                        visitArguments(ctx.arguments()).arguments()
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
        } else {
            return ctx.expr().accept(this);
        }
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
        beginScope();
        Expression thenExpr = ctx.getChild(2).accept(this).toExpr();
        endScope();
        beginScope();
        Expression elseExpr = ctx.getChild(4).accept(this).toExpr();
        endScope();
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.IfElseStatement(
                        List.of(
                                new Expression.IfElseStatement.ConditionalExecution(
                                        ifExpr,
                                        thenExpr
                                )
                        ),
                        Optional.of(
                                elseExpr
                        )
                )
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
        beginScope();
        String varName = ctx.getChild(2).getText();
        initVar(varName);
        Expression expr = ctx.expr().accept(this).toExpr();
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
        List<Expression.IfElseStatement.ConditionalExecution> conditionals = new ArrayList<>();
        for (int i = 0; i < ifClauses.size(); ++i) {
            beginScope();
            Expression ifClause = ifClauses.get(i).accept(this).toExpr();
            endScope();
            beginScope();
            Expression thenExpr = blocks.get(i).accept(this).toExpr();
            endScope();
            conditionals.add(
                    new Expression.IfElseStatement.ConditionalExecution(
                            ifClause,
                            thenExpr
                    )
            );
        }
        Optional<Expression> elseBlock = Optional.empty();
        if (blocks.size() > ifClauses.size()) {
            beginScope();
            elseBlock = Optional.of(
                    blocks.getLast().accept(this).toExpr()
            );
            endScope();
        }
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.IfElseStatement(
                        conditionals,
                        elseBlock
                )
        );
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
        List<Expression.IfElseStatement.ConditionalExecution> conditionals = new ArrayList<>();
        for (int i = 0; i < ifClauses.size(); ++i) {
            beginScope();
            Expression ifClause = ifClauses.get(i).accept(this).toExpr();
            endScope();
            beginScope();
            Expression thenBlock = thenBlocks.get(i).accept(this).toExpr();
            endScope();
            conditionals.add(
                    new Expression.IfElseStatement.ConditionalExecution(
                            ifClause,
                            thenBlock
                    )
            );
        }
        Optional<Expression> elseBlock = Optional.empty();
        if (thenBlocks.size() > ifClauses.size()) {
            beginScope();
            elseBlock = Optional.of(
                    thenBlocks.getLast().accept(this).toExpr()
            );
            endScope();
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
                        "==",
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
                ExpressionFactoryMethods.integer(Integer.valueOf(ctx.NUMBER().getText()))
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
    public ParsableProgramm visitNonTernaryListExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryListExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.ListLiteral(
                        ctx.nonTernaryExpr().stream().map(exprCtx -> exprCtx.accept(this).toExpr()).toList()
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

    // NonTernary expression visitors - delegate to the same logic as regular expressions
    @Override
    public ParsableProgramm visitNonTernaryIfExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryIfExprContext ctx) {
        var ifClauses = ctx.nonTernaryExpr();
        var thenBlocks = ctx.block();
        List<Expression.IfElseStatement.ConditionalExecution> conditionals = new ArrayList<>();
        for (int i = 0; i < ifClauses.size(); ++i) {
            beginScope();
            Expression ifClause = ifClauses.get(i).accept(this).toExpr();
            endScope();
            beginScope();
            Expression thenBlock = thenBlocks.get(i).accept(this).toExpr();
            endScope();
            conditionals.add(
                    new Expression.IfElseStatement.ConditionalExecution(
                            ifClause,
                            thenBlock
                    )
            );
        }
        Optional<Expression> elseBlock = Optional.empty();
        if (thenBlocks.size() > ifClauses.size()) {
            beginScope();
            elseBlock = Optional.of(
                    thenBlocks.getLast().accept(this).toExpr()
            );
            endScope();
        }
        return new ParsableProgramm.ParsableCodeElement(
                new Expression.IfElseStatement(
                        conditionals,
                        elseBlock
                )
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryLogicalOrExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryLogicalOrExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.nonTernaryExpr(0).accept(this).toExpr(),
                        "||",
                        ctx.nonTernaryExpr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryLogicalAndExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryLogicalAndExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.nonTernaryExpr(0).accept(this).toExpr(),
                        "&&",
                        ctx.nonTernaryExpr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryEqExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryEqExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.nonTernaryExpr(0).accept(this).toExpr(),
                        "==",
                        ctx.nonTernaryExpr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryXorExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryXorExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.nonTernaryExpr(0).accept(this).toExpr(),
                        "^",
                        ctx.nonTernaryExpr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryNotExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryNotExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ctx.accept(this).toExpr() // TODO not operator
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryMulDivExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryMulDivExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.nonTernaryExpr(0).accept(this).toExpr(),
                        ctx.getChild(1).getText(),
                        ctx.nonTernaryExpr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryAddSubExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryAddSubExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.expr(
                        ctx.nonTernaryExpr(0).accept(this).toExpr(),
                        ctx.getChild(1).getText(),
                        ctx.nonTernaryExpr(1).accept(this).toExpr()
                )
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryParenExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryParenExprContext ctx) {
        return ctx.nonTernaryExpr().accept(this);
    }

    @Override
    public ParsableProgramm visitNonTernaryVarExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryVarExprContext ctx) {
        String varName = ctx.getText();
        int scopeOffset = checkIfVarIsInitialized(varName);
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.var(varName, scopeOffset)
        );
    }

    @Override
    public ParsableProgramm visitNonTernaryNumberExpr(de.flogehring.peel.antlr.PeelParser.NonTernaryNumberExprContext ctx) {
        return new ParsableProgramm.ParsableCodeElement(
                ExpressionFactoryMethods.integer(Integer.valueOf(ctx.NUMBER().getText()))
        );
    }
}