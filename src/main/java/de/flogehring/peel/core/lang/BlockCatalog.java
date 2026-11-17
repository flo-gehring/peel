package de.flogehring.peel.core.lang;

import de.flogehring.peel.core.values.PeelFunctionDefinition;
import java.util.*;

/** Static block selection hints. Reachability deliberately overestimates dynamic calls. */
public record BlockCatalog(List<Definition> blocks) {
    public BlockCatalog {
        blocks = List.copyOf(blocks);
    }

    public record Definition(String name, String parentName, List<String> reachableBlocks) {
        public Definition {
            reachableBlocks = List.copyOf(reachableBlocks);
        }
    }

    public List<String> names() {
        return blocks.stream().map(Definition::name).toList();
    }

    public static BlockCatalog analyze(Program program) {
        var analysis = new Analysis();
        analysis.collect(program.programm(), null);
        return new BlockCatalog(analysis.named.entrySet().stream().map(entry -> {
            Set<Expression> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<String> reachable = new HashSet<>();
            analysis.reach(entry.getValue(), visited, reachable);
            return new Definition(entry.getKey(), analysis.parents.get(entry.getKey()),
                    analysis.named.keySet().stream().filter(reachable::contains).toList());
        }).toList());
    }

    private static final class Analysis {
        private final Map<String, Expression.Block> named = new LinkedHashMap<>();
        private final Map<String, String> parents = new HashMap<>();
        private final Map<String, List<Expression.Block>> functions = new HashMap<>();
        private final Set<String> variableNames = new HashSet<>();
        private final List<Expression.Block> callableBodies = new ArrayList<>();

        void collect(Expression expression, String parent) {
            if (expression instanceof Expression.Block block && block.name() != null) {
                if (named.putIfAbsent(block.name(), block) != null)
                    throw new IllegalArgumentException("Block name '" + block.name() + "' is already declared in this script");
                parents.put(block.name(), parent);
                parent = block.name();
            }
            if (expression instanceof Expression.Assignment assignment) variableNames.add(assignment.variableName());
            if (expression instanceof Expression.ForEachLoop loop) variableNames.add(loop.varName());
            PeelFunctionDefinition callable = callable(expression);
            if (callable != null) {
                callableBodies.add(callable.getBody());
                variableNames.addAll(callable.getParameters());
                if (expression instanceof Expression.FunctionDeclaration)
                    functions.computeIfAbsent(callable.getName(), ignored -> new ArrayList<>()).add(callable.getBody());
                collect(callable.getBody(), parent);
            }
            for (Expression child : children(expression)) collect(child, parent);
        }

        void reach(Expression expression, Set<Expression> visited, Set<String> result) {
            if (!visited.add(expression)) return;
            if (expression instanceof Expression.Block block && block.name() != null) result.add(block.name());
            for (Expression child : children(expression)) reach(child, visited, result);
            if (expression instanceof Expression.FunctionCall call) {
                Collection<Expression.Block> targets;
                if (callable(call.callee()) != null) {
                    targets = List.of(callable(call.callee()).getBody());
                } else if (call.callee() instanceof Expression.VariableName name
                        && name.scopeOffset() != -1
                        && functions.containsKey(name.name()) && !variableNames.contains(name.name())) {
                    // All declarations with this name are candidates, including overloads and shadows.
                    targets = functions.get(name.name());
                } else {
                    // A parameter, selector, host function or returned callable may invoke any script body.
                    targets = callableBodies;
                }
                for (Expression.Block target : targets) reach(target, visited, result);
            }
        }
    }

    private static PeelFunctionDefinition callable(Expression expression) {
        if (expression instanceof Expression.FunctionDeclaration declaration) return declaration.callable();
        if (expression instanceof Expression.Literal literal && literal.value() instanceof PeelFunctionDefinition function)
            return function;
        return null;
    }

    private static List<Expression> children(Expression expression) {
        return switch (expression) {
            case Expression.Block block -> block.codeElements();
            case Expression.Assignment assignment -> List.of(assignment.assignment());
            case Expression.FunctionDeclaration ignored -> List.of();
            case Expression.IfElseStatement conditional -> {
                var children = new ArrayList<Expression>();
                conditional.conditionals().forEach(branch -> {
                    children.add(branch.condition());
                    children.add(branch.then());
                });
                conditional.elseExpr().ifPresent(children::add);
                yield children;
            }
            case Expression.ListLiteral list -> list.elements();
            case Expression.MapLiteral map -> map.entries().stream()
                    .flatMap(entry -> java.util.stream.Stream.of(entry.key(), entry.value())).toList();
            case Expression.Selector selector -> List.of(selector.target(), selector.selector());
            case Expression.BinaryOperator operator -> List.of(operator.lhs(), operator.rhs());
            case Expression.UnaryPrefixOperator operator -> List.of(operator.arg());
            case Expression.FunctionCall call -> {
                var children = new ArrayList<Expression>();
                children.add(call.callee());
                children.addAll(call.arguments());
                yield children;
            }
            case Expression.WhileLoop loop -> List.of(loop.condition(), loop.body());
            case Expression.ForEachLoop loop -> List.of(loop.list(), loop.body());
            case Expression.Return returned -> List.of(returned.value());
            case Expression.Literal ignored -> List.of();
            case Expression.VariableName ignored -> List.of();
        };
    }
}
