package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.PeelValue;

import java.util.List;
import java.util.Optional;

public class EvaluationEnvironment {

    private final Scope global;
    private final Optional<EvaluationEnvironment> parent;
    private final List<Scope> scopes;

    public EvaluationEnvironment(Scope global, Optional<EvaluationEnvironment> parent, List<Scope> scopes) {
        this.global = global;
        this.parent = parent;
        this.scopes = scopes;
    }

    public void put(Expression.VariableName varName, PeelValue value) {
        getScopeBy(varName.scopeOffset()).putVar(varName.name(), value);
    }

    private int getScopeIndexBy(int scopeOffset) {
        return scopes.size() - 1 - scopeOffset;
    }

    private Scope getScopeBy(int scopeOffset) {
        return scopes.get(getScopeIndexBy(scopeOffset));
    }

    PeelValue getVar(Expression.VariableName varName) {
        return getVar(varName.name(), varName.scopeOffset());
    }

    boolean isFunction(Expression.VariableName name) {
        return !global.getFunction(name.name()).isEmpty();
    }

    boolean isVar(Expression.VariableName variableName) {
        var name = variableName.name();
        var scopeOffset = variableName.scopeOffset();
        if (scopeOffset == -1) {
            return global.hasVar(name);
        } else {
            return getScopeBy(scopeOffset).hasVar(name);
        }

    }

    List<Function> getFunction(Expression.VariableName varName) {
        var name = varName.name();
        var offset = varName.scopeOffset();
        return global.getFunction(name);
    }

    void enterScope() {
        scopes.add(Scope.empty());
    }

    void exitScope() {
        scopes.removeLast();
    }

    private PeelValue getVar(String name, int scopeOffset) {
        if (scopeOffset == -1) {
            return global.getVar(name);
        } else {
            return getScopeBy(scopeOffset).getVar(name);
        }
    }

    public List<Function> getFunction(String operator) {
        return global.getFunction(operator);
    }
}