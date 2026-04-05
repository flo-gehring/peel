package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.eval.Variable;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.UndefinedVarException;

import java.util.*;
import java.util.stream.Stream;

public class Scope {

    private final HashMap<String, PeelValue> variables;
    private final HashMap<String, List<Function>> functions;
    private final HashMap<String, List<OperatorDef>> operators;

    private Scope(
            HashMap<String, PeelValue> variables,
            HashMap<String, List<Function>> functions,
            HashMap<String, List<OperatorDef>> operators
    ) {
        this.variables = variables;
        this.functions = functions;
        this.operators = operators;
    }

    public static Scope empty() {
        return new Scope(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public static Scope from(
            List<Variable> variables,
            List<Function> functions,
            List<OperatorDef> operators
    ) {
        Scope scope = empty();
        variables.forEach(scope::register);
        functions.forEach(scope::register);
        operators.forEach(scope::register);
        return scope;
    }

    public Scope copy() {
        HashMap<String, PeelValue> variableCopy = new HashMap<>(variables);
        HashMap<String, List<Function>> functionCopy = new HashMap<>();
        for (Map.Entry<String, List<Function>> entry : functions.entrySet()) {
            functionCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        HashMap<String, List<OperatorDef>> operatorCopy = new HashMap<>();
        for (Map.Entry<String, List<OperatorDef>> entry : operators.entrySet()) {
            operatorCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new Scope(variableCopy, functionCopy, operatorCopy);
    }

    public void register(Function f) {
        String name = f.name();
        registerFunction(f, name);
    }

    private void registerFunction(Function f, String name) {
        functions.merge(name, new ArrayList<>(List.of(f)), (lhs, rhs) -> Stream.concat(
                lhs.stream(),
                rhs.stream()
        ).toList());
    }

    public void register(String name, Function f) {
        registerFunction(f, name);
    }

    public void register(Variable v) {
        variables.put(v.name(), v.value());
    }

    public void register(OperatorDef operatorDef) {
        operators.merge(
                operatorDef.symbol(),
                new ArrayList<>(List.of(operatorDef)),
                (lhs, rhs) -> Stream.concat(lhs.stream(), rhs.stream()).toList()
        );
    }

    public PeelValue getVar(String name) {
        if (!variables.containsKey(name)) {
            throw new UndefinedVarException("Undefined Variable: " + name);
        }
        return variables.get(name);
    }

    public boolean hasVar(String var) {
        return variables.containsKey(var);
    }

    public Map<String, PeelValue> variablesView() {
        return Collections.unmodifiableMap(variables);
    }

    public void putVar(String name, PeelValue value) {
        variables.put(name, value);
    }

    public List<Function> getFunction(String s) {
        return functions.getOrDefault(s, List.of());
    }

    public boolean hasFunction(Expression.VariableName variableName) {
        return functions.containsKey(variableName.name());
    }

    public List<OperatorDef> getOperators(String symbol) {
        return operators.getOrDefault(symbol, List.of());
    }
}
