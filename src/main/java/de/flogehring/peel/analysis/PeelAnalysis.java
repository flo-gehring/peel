package de.flogehring.peel.analysis;

import de.flogehring.peel.antlr.PeelBaseVisitor;
import de.flogehring.peel.antlr.PeelLexer;
import de.flogehring.peel.antlr.PeelParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.*;
import static de.flogehring.peel.analysis.AnalysisResult.*;

public final class PeelAnalysis {
    private PeelAnalysis() {}

    public static boolean isIdentifier(String name) {
        if (name == null || !name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) return false;
        var lexer = new PeelLexer(CharStreams.fromString(name));
        lexer.removeErrorListeners();
        return lexer.nextToken().getType() == PeelLexer.IDENT && lexer.nextToken().getType() == Token.EOF;
    }

    public static AnalysisResult analyze(String source, List<ProvidedSymbol> externalSymbols,
                                         List<ProvidedSymbol> runtimeSymbols) {
        Objects.requireNonNull(source, "source");
        var visitor = new Analyzer(source);
        runtimeSymbols.forEach(symbol -> visitor.provide(symbol, "runtime"));
        externalSymbols.forEach(symbol -> visitor.provide(symbol, "external"));
        var lexer = new PeelLexer(CharStreams.fromString(source));
        var parser = new PeelParser(new CommonTokenStream(lexer));
        var errors = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> recognizer, Object offending, int line,
                    int column, String message, RecognitionException exception) {
                int start = offending instanceof Token token ? visitor.start(token) :
                        visitor.offset(((Lexer) recognizer).getCharIndex());
                int end = offending instanceof Token token ? visitor.end(token) : Math.min(source.length(), start + 1);
                visitor.diagnostics.add(new Diagnostic("syntax_error", "error", message, start, end));
            }
        };
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errors);
        parser.addErrorListener(errors);
        var program = parser.program();
        visitor.inScope(program, () -> program.statement().forEach(visitor::visit));
        return new AnalysisResult(List.copyOf(visitor.symbols), List.copyOf(visitor.occurrences),
                List.copyOf(visitor.scopes), List.copyOf(visitor.diagnostics));
    }

    private static final class Analyzer extends PeelBaseVisitor<Void> {
        private final String source;
        private final int[] offsets;
        private final List<Symbol> symbols = new ArrayList<>();
        private final List<Occurrence> occurrences = new ArrayList<>();
        private final List<LexicalScope> scopes = new ArrayList<>();
        private final List<Diagnostic> diagnostics = new ArrayList<>();
        private final List<Map<String, Integer>> bindings = new ArrayList<>();
        private final Set<Integer> uninitialized = new HashSet<>();
        private int scopeId;
        private int functionDepth;

        Analyzer(String source) {
            this.source = source;
            offsets = new int[source.codePointCount(0, source.length()) + 1];
            for (int cp = 0, utf16 = 0; cp < offsets.length; cp++) {
                offsets[cp] = utf16;
                if (utf16 < source.length()) utf16 += Character.charCount(source.codePointAt(utf16));
            }
            scopes.add(new LexicalScope(0, -1, 0, source.length(), true));
            bindings.add(new LinkedHashMap<>());
        }
        int offset(int codepoint) { return offsets[Math.clamp(codepoint, 0, offsets.length - 1)]; }
        int start(Token token) { return offset(token.getStartIndex()); }
        int end(Token token) { return offset(Math.max(token.getStartIndex(), token.getStopIndex() + 1)); }
        boolean real(TerminalNode node) { return node != null && node.getSymbol().getTokenIndex() >= 0; }

        void provide(ProvidedSymbol provided, String origin) {
            if (!isIdentifier(provided.name())) throw new IllegalArgumentException("Invalid symbol name: " + provided.name());
            var previous = bindings.getFirst().get(provided.name());
            // Runtime function overloads share a name; global function reads take precedence over values.
            if (previous != null && symbols.get(previous).kind().equals("function")) return;
            int id = symbols.size();
            symbols.add(new Symbol(id, provided.name(), provided.kind(), origin,
                    Objects.requireNonNullElse(provided.description(), ""), provided.arity(), 0, 0, 0, 0));
            bindings.getFirst().put(provided.name(), id);
        }
        void inScope(ParserRuleContext context, Runnable action) {
            int parent = scopeId;
            scopeId = scopes.size();
            int stop = context.getStop() == null ? source.length() : end(context.getStop());
            boolean openEnded = context instanceof PeelParser.ProgramContext ||
                    (broken(context) && !"}".equals(context.getStop() == null ? "" : context.getStop().getText()));
            scopes.add(new LexicalScope(scopeId, parent, start(context.getStart()),
                    openEnded ? source.length() : stop, openEnded));
            bindings.add(new LinkedHashMap<>());
            try { action.run(); } finally { scopeId = parent; }
        }
        void diagnostic(String code, String severity, String message, Token token) {
            diagnostics.add(new Diagnostic(code, severity, message, start(token), end(token)));
        }
        Integer declare(TerminalNode node, String kind, int arity) {
            if (!real(node)) return null;
            var token = node.getSymbol();
            var name = node.getText();
            var previous = bindings.get(scopeId).get(name);
            if (previous != null) {
                if (kind.equals("function") && symbols.get(previous).kind().equals("function")) {
                    occurrences.add(new Occurrence(previous, "declaration", start(token), end(token)));
                    return previous;
                }
                diagnostic("duplicate_declaration", "error", "Name already declared in this scope: " + name, token);
                return null;
            }
            int id = symbols.size();
            symbols.add(new Symbol(id, name, kind, "local", "", arity, scopeId, end(token), start(token), end(token)));
            bindings.get(scopeId).put(name, id);
            occurrences.add(new Occurrence(id, "declaration", start(token), end(token)));
            return id;
        }
        Integer resolve(String name) {
            for (int id = scopeId; id >= 0; id = scopes.get(id).parentId()) {
                var found = bindings.get(id).get(name);
                if (found != null) return found;
            }
            return null;
        }
        void reference(TerminalNode node, String role) {
            if (!real(node)) return;
            Integer id = resolve(node.getText());
            if (id == null) {
                diagnostic(role.equals("assignment") ? "undeclared_assignment" : "unknown_name",
                        role.equals("assignment") ? "error" : "warning",
                        role.equals("assignment") ? "Cannot assign to undeclared variable: " + node.getText() :
                                "Unknown name; declare it or add an expected binding: " + node.getText(), node.getSymbol());
                return;
            }
            if (role.equals("assignment") && symbols.get(id).scopeId() == 0)
                diagnostic("undeclared_assignment", "error", "Cannot assign to an external variable: " + node.getText(), node.getSymbol());
            else if (uninitialized.contains(id))
                diagnostic("uninitialized_variable", "error", "Cannot read uninitialized variable: " + node.getText(), node.getSymbol());
            occurrences.add(new Occurrence(id, role, start(node.getSymbol()), end(node.getSymbol())));
        }
        boolean broken(ParseTree tree) {
            if (tree instanceof ErrorNode) return true;
            if (tree instanceof ParserRuleContext context && context.exception != null) return true;
            for (int i = 0; i < tree.getChildCount(); i++) if (broken(tree.getChild(i))) return true;
            return false;
        }
        @Override public Void visitDeclaration(PeelParser.DeclarationContext context) {
            if (broken(context)) return null;
            var id = declare(context.IDENT(), "variable", -1);
            if (id != null) uninitialized.add(id);
            if (context.expr() == null) return null;
            visit(context.expr());
            if (id != null) uninitialized.remove(id);
            return null;
        }

        @Override public Void visitStatement(PeelParser.StatementContext context) {
            if (context.expr() != null && broken(context.expr())) return null;
            return super.visitStatement(context);
        }
        @Override public Void visitFunctionDeclaration(PeelParser.FunctionDeclarationContext context) {
            declare(context.IDENT(), "function", context.parameters() == null ? -1 : context.parameters().IDENT().size());
            function(context, context.parameters(), context.block());
            return null;
        }
        private void function(ParserRuleContext context, PeelParser.ParametersContext parameters, PeelParser.BlockContext body) {
            if (body == null || parameters == null) return;
            inScope(context, () -> {
                parameters.IDENT().forEach(node -> declare(node, "parameter", -1));
                functionDepth++;
                try { visit(body); } finally { functionDepth--; }
            });
        }
        @Override public Void visitLambdaExpr(PeelParser.LambdaExprContext context) {
            function(context, context.parameters(), context.block());
            return null;
        }
        @Override public Void visitBlock(PeelParser.BlockContext context) {
            inScope(context, () -> context.statement().forEach(this::visit));
            return null;
        }
        @Override public Void visitForEachStatement(PeelParser.ForEachStatementContext context) {
            if (context.expr() != null && !broken(context.expr())) visit(context.expr());
            if (context.block() != null) inScope(context.block(), () -> {
                declare(context.IDENT(), "variable", -1);
                visit(context.block());
            });
            return null;
        }
        @Override public Void visitAssignment(PeelParser.AssignmentContext context) {
            if (broken(context)) return null;
            if (context.expr() != null) visit(context.expr());
            reference(context.IDENT(), "assignment");
            return null;
        }
        @Override public Void visitReturnStatement(PeelParser.ReturnStatementContext context) {
            if (functionDepth == 0)
                diagnostic("return_outside_function", "error", "Return is only allowed inside a function", context.getStart());
            if (context.expr() != null && !broken(context.expr())) visit(context.expr());
            return null;
        }
        @Override public Void visitVarExpr(PeelParser.VarExprContext context) {
            reference(context.IDENT(), "reference");
            return null;
        }
        @Override public Void visitFunctionCallExpr(PeelParser.FunctionCallExprContext context) {
            if (context.expr() instanceof PeelParser.VarExprContext variable) reference(variable.IDENT(), "call");
            else if (context.expr() != null) visit(context.expr());
            if (context.arguments() != null) visit(context.arguments());
            return null;
        }
    }
}
