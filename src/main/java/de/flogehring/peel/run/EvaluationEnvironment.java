package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.PeelException;

import java.util.ArrayList;
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

    public EvaluationEnvironment copy() {
        return new EvaluationEnvironment(
                global,
                parent,
                new ArrayList<>(scopes)
        );
    }

    private int getScopeIndexBy(int scopeOffset) {
        return scopes.size() - 1 - scopeOffset;
    }

    private Scope getScopeBy(int scopeOffset) {
        int scopeIndex = getScopeIndexBy(scopeOffset);
        if (scopeIndex >= 0) {
            return scopes.get(scopeIndex);
        } else {
            EvaluationEnvironment parentEnv = parent.orElseThrow(
                    () -> new PeelException("Can't resolve Variable, no Parent scope")
            );
            return parentEnv.getScopeBy(scopeOffset - scopes.size());
        }
    }

    PeelValue getVar(Expression.VariableName varName) {
        return getVar(varName.name(), varName.scopeOffset());
    }

    boolean isGlobalFunction(Expression.VariableName name) {
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
            Scope scopeBy = getScopeBy(scopeOffset);
            if (scopeBy.hasVar(name)) {
                return scopeBy.getVar(name);
            } else if (parent.isPresent()) { // TODO This can solved more efficiently
                // Potentially in Recursive Call
                return tryFindVarMovingUp(name, getScopeIndexBy(scopeOffset));
            } else {
                throw new PeelException("Undefined Var with name {0}", name);
            }
        }
    }

    private PeelValue tryFindVarMovingUp(String name, int lowerScopeBound) {
        for (int i = lowerScopeBound; i >= 0; --i) {
            Scope scope = scopes.get(i);
            if (scope.hasVar(name)) {
                return scope.getVar(name);
            }
        }
        if (parent.isPresent()) {
            EvaluationEnvironment evaluationEnvironment = parent.get();
            return evaluationEnvironment.tryFindVarMovingUp(name, evaluationEnvironment.scopes.size() - 1);
        } else {
            throw new PeelException("Undefined Var with name {0}", name);
        }
    }

    public List<Function> getFunction(String operator) {
        return global.getFunction(operator);
    }

    public void putFunction(Function callable) {
        global.register(callable);
    }

    public void putFunction(Expression.VariableName varName, Function function) {
        getScopeBy(varName.scopeOffset()).register(varName.name(), function);
    }

    public EvaluationEnvironment spawnChild() {
        return new EvaluationEnvironment(
                global,
                Optional.of(this),
                new ArrayList<>()
        );
    }

    public List<Function> getLocalFunction(Expression.VariableName variableName) {
        return getScopeBy(variableName.scopeOffset()).getFunction(variableName.name());
    }

    public boolean isLocalFunction(Expression.VariableName variableName) {
        return getScopeBy(variableName.scopeOffset()).hasFunction(variableName);
    }
}