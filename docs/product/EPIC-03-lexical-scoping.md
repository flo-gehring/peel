# Epic 3: Lexical Scoping

**Status**: Not Started
**Dependencies**: Epic 1 (Branching), Epic 2 (Loops)
**Goal**: Implement proper lexical scoping with nested scopes, variable shadowing, and scope-based variable lifetime.

## Overview

This epic is a **major refactoring** of `SimpleRuntime`. Currently, all variables live in a flat
`HashMap<String, EvaluatedExpression>`. We need to introduce a scope chain to support:

- Block-level scoping
- Variable shadowing
- Proper variable lifetime
- Foundation for closures (Epic 4)

This is critical infrastructure for first-class functions.

## Features

### 1. Block Scopes

- **Description**: Each `{ }` block creates a new scope
- **Behavior**:
    - Variables declared in a block are only visible within that block
    - Inner blocks can access outer block variables
    - Variables are destroyed when block exits
- **Test cases**:
    - Variable in block not visible after block
    - Nested blocks access outer variables
    - Variable assignment in inner block affects outer scope if variable exists there

### 2. Variable Shadowing

- **Description**: Inner scope can declare variable with same name as outer scope
- **Behavior**:
    - Inner variable hides outer variable
    - Outer variable unchanged by inner scope
    - Original variable visible again after inner scope exits
- **Test cases**:
    - Shadow variable in if block
    - Shadow variable in loop
    - Multiple levels of shadowing

### 3. Scope Types

Different statement types create scopes:

- **Block scope**: `{ statements }`
- **If scope**: `if (cond) { }` and `else { }`
- **Loop scope**: `while (cond) { }`, `for (...) { }`
- **Function scope**: Function body (Epic 4)

### 4. Assignment vs Declaration

Currently assignment creates variables. Need to decide:

- **Option A**: `x = 5` always creates/assigns in current scope
- **Option B**: `let x = 5` declares, `x = 5` assigns to existing
- **Option C**: `x = 5` creates if new, assigns to nearest scope if exists

**Decision needed**: Choose scoping semantics

## Technical Implementation

### Scope Data Structure

Replace flat HashMap with scope chain:

```java
class Scope {
    private final Scope parent;  // null for global scope
    private final HashMap<String, EvaluatedExpression> variables;

    Scope(Scope parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
    }

    // Look up variable in this scope and parent scopes
    Optional<EvaluatedExpression> lookup(String name) {
        if (variables.containsKey(name)) {
            return Optional.of(variables.get(name));
        }
        return parent != null ? parent.lookup(name) : Optional.empty();
    }

    // Assign to existing variable in scope chain, or create in current scope
    void assign(String name, EvaluatedExpression value) {
        if (variables.containsKey(name)) {
            variables.put(name, value);
        } else if (parent != null && parent.lookup(name).isPresent()) {
            parent.assign(name, value);
        } else {
            variables.put(name, value);  // create new
        }
    }

    // Declare in current scope (for shadowing)
    void declare(String name, EvaluatedExpression value) {
        variables.put(name, value);
    }

    // Create child scope
    Scope child() {
        return new Scope(this);
    }
}
```

### Runtime Refactoring

**Current**:

```java
private final HashMap<String, EvaluatedExpression> variables;
```

**New**:

```java
private Scope currentScope;  // starts as global scope
```

All variable operations need to use scope:

- `evaluateExpr(Expression.VariableName)` → `currentScope.lookup(name)`
- `runStatement(Statement.Assignment)` → `currentScope.assign(name, value)`

### Scope Management

Need to push/pop scopes when entering/exiting blocks:

```java
private EvaluatedStatement runBlock(List<CodeElement> block) {
    Scope previousScope = currentScope;
    currentScope = currentScope.child();  // enter new scope

    try {
        List<EvaluatedCodeElement> evaluated = new ArrayList<>();
        for (CodeElement element : block) {
            // evaluate element
        }
        return new EvaluatedStatement.Block(evaluated);
    } finally {
        currentScope = previousScope;  // restore parent scope
    }
}
```

Apply this pattern to:

- If/else blocks
- While loop bodies
- For loop bodies (and for-init scope)
- Function bodies (Epic 4)

### Grammar Changes (Optional)

If we want explicit declaration vs assignment:

```antlr
statement
    : declaration
    | assignment
    | ...
    ;

declaration
    : 'let' IDENT '=' expr ';'
    ;

assignment
    : IDENT '=' expr ';'
    ;
```

### AST Changes (Optional)

If adding declaration:

```java
record Declaration(String name, Expression value) implements Statement {}
```

## Test-Driven Development

### Refactoring Strategy

1. **Create Scope class** with tests
2. **Refactor SimpleRuntime** to use Scope for global variables (no new scopes yet)
3. **Add block scoping** for if/else
4. **Add block scoping** for loops
5. **Add shadowing tests**

### Test Order

1. **ScopeTest**: Unit tests for Scope class
    - lookup in current scope
    - lookup in parent scope
    - assign creates variable
    - assign updates existing variable
    - shadowing with declare
2. **BlockScopeTest**: Block-level scoping
3. **IfScopeTest**: If/else block scoping
4. **LoopScopeTest**: Loop body scoping
5. **ShadowingTest**: Variable shadowing
6. **ExistingTestsStillPass**: Ensure Epic 1 & 2 tests still work

### Example Test Cases

```java
@Test
void blockScopeVariableNotVisibleAfter() {
    Program program = PeelGrammar.parse("""
        x = 5;
        if (true) {
            y = 10;
        }
        y;  // Should throw: variable not found
    """);
    SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
    assertThrows(NoVariableFoundException.class, () -> runtime.run(program));
}

@Test
void variableShadowing() {
    Program program = PeelGrammar.parse("""
        x = 5;
        if (true) {
            x = 10;  // shadows outer x (or assigns if we do lookup?)
        }
        x;  // Should be 5 if shadowing, 10 if assignment
    """);
    // Test depends on decision about assignment semantics
}

@Test
void nestedScopeAccess() {
    Program program = PeelGrammar.parse("""
        x = 5;
        if (true) {
            y = x + 10;  // Can read x from outer scope
        }
        // y not visible here
    """);
    // Should succeed
}

@Test
void forLoopVariableScoping() {
    Program program = PeelGrammar.parse("""
        for (i = 0; i < 5; i = i + 1) {
            // i visible here
        }
        i;  // Should i be visible here?
    """);
    // Decision needed: for-loop variable scope
}
```

## Open Questions

1. **Assignment semantics**:
    - Should `x = 5` create new variable or assign to existing?
    - JavaScript: assigns to existing if found, creates global if not (bad)
    - Python: creates in current scope
    - Lua: needs `local` keyword
    - **Recommendation**: Look up scope chain, assign if exists, create in current scope if not

2. **Declaration keyword**:
    - Add `let` or similar for explicit declaration?
    - **Recommendation**: Not for MVP, just use assignment semantics

3. **For loop variable scope**:
    - C-style: `for (i = 0; ...)` - where does `i` live?
    - Option A: `i` lives in loop body scope only
    - Option B: `i` lives in parent scope
    - Option C: for-init creates its own scope around loop
    - **Recommendation**: Option C (most languages do this)

4. **Global scope**:
    - Should there be a global scope for registered variables/functions?
    - **Answer**: Yes, `Runtime.register()` puts things in global scope

5. **Const/immutability**:
    - Support immutable variables?
    - **Recommendation**: Not for MVP, all variables mutable

## Definition of Done

- [ ] Scope class implemented and tested
- [ ] SimpleRuntime refactored to use Scope
- [ ] Block statements create new scopes
- [ ] If/else blocks create new scopes
- [ ] Loop bodies create new scopes
- [ ] Variable shadowing works correctly
- [ ] All Epic 1 & 2 tests still pass
- [ ] New scoping tests pass
- [ ] Variable lookup errors give good messages
- [ ] Documentation updated with scoping rules

## Notes for Next Epic

Epic 4 (First-Class Functions) will build on this infrastructure:

- Function definitions capture current scope (closures)
- Function calls create new scope for parameters and body
- Returned functions carry their closure scope
