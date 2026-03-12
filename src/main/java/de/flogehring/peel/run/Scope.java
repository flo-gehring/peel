package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.eval.Variable;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.UndefinedVarException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class Scope {

    private final HashMap<String, PeelValue> variables;
    private final HashMap<String, List<Function>> functions;

    private Scope(HashMap<String, PeelValue> variables, HashMap<String, List<Function>> functions) {
        this.variables = variables;
        this.functions = functions;
    }

    public static Scope empty() {
        return new Scope(new HashMap<>(), new HashMap<>());
    }

    public void register(Function f) {
        functions.merge(f.name(), new ArrayList<>(List.of(f)), (lhs, rhs) -> Stream.concat(
                lhs.stream(),
                rhs.stream()
        ).toList());
    }

    public void register(Variable v) {
        variables.put(v.name(), v.value());
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
    public void putVar(String name, PeelValue value) {
        variables.put(name, value);
    }

    public List<Function> getFunction(String s) {
        return functions.getOrDefault(s, List.of());
    }
}
