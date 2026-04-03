package de.flogehring.peel.run;

import de.flogehring.peel.core.eval.Function;
import de.flogehring.peel.core.lang.Expression;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.run.exceptions.PeelException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// TODO Should Peel Closure Values hold the Reference to the evaluation environment?
public class EvaluationEnvironment {

    private final Scope global;
    private final Optional<EvaluationEnvironment> parent;
    private final List<Scope> scopes;

    public EvaluationEnvironment(Scope global, Optional<EvaluationEnvironment> parent, List<Scope> scopes) {
        this.global = global;
        this.parent = parent;
        this.scopes = scopes;
    }

    void enterScope() {
        scopes.add(Scope.empty());
    }

    void exitScope() {
        scopes.removeLast();
    }

    void put(Expression.VariableName varName, PeelValue value) {
        getScopeBy(varName.scopeOffset()).putVar(varName.name(), value);
    }

    void putFunction(Expression.VariableName varName, Function function) {
        getScopeBy(varName.scopeOffset()).register(varName.name(), function);
    }

    List<Function> getFunction(Expression.VariableName varName) {
        return getScopeBy(varName.scopeOffset()).getFunction(varName.name());
    }

    boolean isFunction(Expression.VariableName varName) {
        return getScopeBy(varName.scopeOffset()).hasFunction(varName);
    }

    PeelValue getVar(Expression.VariableName varName) {
        return getVar(varName.name(), varName.scopeOffset());
    }

    List<Function> getOperator(String operator) {
        return global.getFunction(operator);
    }

    EvaluationEnvironment copy() {
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
        if (scopeOffset == -1) {
            return global;
        }
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

    public EvaluationEnvironment spawnChild() {
        return new EvaluationEnvironment(
                global,
                Optional.of(this),
                new ArrayList<>()
        );
    }
}