# Epic 4: First-Class Functions

**Status**: Not Started
**Dependencies**: Epic 3 (Lexical Scoping) - absolutely requires scope chain for closures
**Goal**: Add user-defined functions as first-class values with closure support.

## Overview

This epic completes the "complete programming language" goal by adding function definitions. Functions in PEEL are
first-class values (can be assigned to variables, passed as arguments, returned from functions) and support closures (
capture variables from defining scope).

This is complex because it involves:

- Function definition syntax
- Function values (different from current `Function` interface)
- Closure capture
- Parameter binding
- Return values
- Recursive functions

## Features

### 1. Function Definition

- **Description**: Define a function and assign it to a variable
- **Syntax**: `fn(param1, param2) { statements; return expr; }`
- **Behavior**:
    - Creates a function value
    - Captures current scope (closure)
    - Can be assigned to variable or passed around
- **Test cases**:
    - Define and call simple function
    - Function with multiple parameters
    - Function with no parameters
    - Function returning value

### 2. Function Calls

- **Description**: Call a function value (currently supports built-in functions only)
- **Current**: `Expression.FunctionCall` exists but only for registered functions
- **Enhancement**: Support calling user-defined functions
- **Test cases**:
    - Call user-defined function
    - Call function stored in variable
    - Call function returned from another function
    - Pass function as argument

### 3. Closures

- **Description**: Functions capture variables from defining scope
- **Behavior**:
    - Function "closes over" variables in outer scopes
    - Captured variables remain accessible even after outer scope exits
    - Each closure has independent captured state
- **Test cases**:
    - Function captures outer variable
    - Multiple closures capture different values
    - Closure modifies captured variable
    - Nested closures

### 4. Return Statement

- **Description**: Exit function and return a value
- **Syntax**: `return expr;`
- **Behavior**:
    - Exits function immediately
    - Returns value to caller
    - Functions without explicit return return last expression or void
- **Test cases**:
    - Early return from function
    - Return in nested if/loop
    - Function without return

### 5. Recursion

- **Description**: Function can call itself
- **Behavior**: Function name must be in scope during definition
- **Test cases**:
    - Simple recursion (factorial, fibonacci)
    - Mutual recursion (if supported)

## Technical Implementation

### Function Value Type

Need new value type for user-defined functions:

```java
// In PeelValue or new location
record FunctionValue(
    List<String> parameters,
    List<CodeElement> body,
    Scope closureScope,  // captured scope
    String name  // optional, for recursion
) implements PeelValue {}
```

### Grammar Changes

```antlr
statement
    : ...
    | returnStatement
    ;

returnStatement
    : 'return' expr? ';'
    ;

expr
    : functionDefinition
    | ...
    ;

functionDefinition
    : 'fn' '(' paramList? ')' block
    ;

paramList
    : IDENT (',' IDENT)*
    ;
```

### AST Changes

```java
// Expression variant for function definition
record FunctionDefinition(
    List<String> parameters,
    List<CodeElement> body
) implements Expression {}

// Statement for return
record ReturnStatement(Expression value) implements Statement {}  // nullable value
```

### Runtime Changes

**Major changes** to handle function values:

1. **Evaluate function definition**:
   ```java
   case Expression.FunctionDefinition(var params, var body) -> {
       // Capture current scope for closure
       Scope closureScope = currentScope.snapshot();  // or reference
       return new EvaluatedExpression.Function(
           new FunctionValue(params, body, closureScope, null)
       );
   }
   ```

2. **Call user-defined function**:
   ```java
   private EvaluatedExpression callUserFunction(
       FunctionValue fn,
       List<EvaluatedExpression> arguments
   ) {
       // Create new scope from closure scope
       Scope functionScope = fn.closureScope().child();

       // Bind parameters
       for (int i = 0; i < fn.parameters().size(); i++) {
           functionScope.declare(fn.parameters().get(i), arguments.get(i));
       }

       // Execute body in function scope
       Scope previousScope = currentScope;
       currentScope = functionScope;
       try {
           // Run body, handle return
           for (CodeElement element : fn.body()) {
               if (element instanceof Statement.ReturnStatement ret) {
                   return evaluateExpr(ret.value());
               }
               // evaluate element
           }
           // implicit return: last expression or void
       } finally {
           currentScope = previousScope;
       }
   }
   ```

3. **Handle return statement**:
    - Need non-local control flow (like break/continue)
    - Options: Exception, special return value, or control flow flag
    - **Recommendation**: Use `ReturnException` with value

4. **Distinguish built-in vs user-defined functions**:
    - Current: `HashMap<String, List<Function>>` for built-ins
    - New: User functions are `FunctionValue` instances
    - Function calls need to check both

### Scope.snapshot() for Closures

Closures need to capture scope. Options:

1. **Reference**: Store reference to scope (mutable capture)
2. **Deep copy**: Clone scope at definition (immutable capture)
3. **Hybrid**: Copy-on-write

**Decision needed**: What semantics do we want?

- Most languages use reference (mutable capture)
- Simpler to implement

### Recursion Support

For recursion, function needs to call itself by name:

```javascript
factorial = fn(n) {
    if (n <= 1) {
        return 1;
    }
    return n * factorial(n - 1);
}
```

Problem: `factorial` isn't in scope during function body.

Solutions:

1. **Assignment creates binding first**: `factorial` is in scope but uninitialized
2. **Letrec semantics**: Function definition includes name
3. **Fix-point**: User needs to use Y-combinator (too complex)

**Recommendation**: Option 1 - assignment declares name before evaluating RHS

## Test-Driven Development

### Test Order

1. **FunctionDefinitionTest**: Parse and create function values
2. **FunctionCallTest**: Call simple functions
3. **FunctionParameterTest**: Parameter binding
4. **FunctionReturnTest**: Return statements
5. **ClosureTest**: Capture outer variables
6. **RecursionTest**: Recursive functions
7. **HigherOrderTest**: Functions as values
8. **ExistingTestsStillPass**: Ensure built-in functions still work

### Example Test Cases

```java
@Test
void simpleFunctionDefinition() {
    Program program = PeelGrammar.parse("""
        add = fn(a, b) {
            return a + b;
        }
        add(3, 5);
    """);
    SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
    EvaluatedProgram result = runtime.run(program);
    // Assert result is 8
}

@Test
void closureCapture() {
    Program program = PeelGrammar.parse("""
        x = 10;
        makeAdder = fn(y) {
            return fn(z) {
                return x + y + z;
            }
        }
        add5 = makeAdder(5);
        add5(3);
    """);
    // Assert result is 18 (10 + 5 + 3)
}

@Test
void recursiveFactorial() {
    Program program = PeelGrammar.parse("""
        factorial = fn(n) {
            if (n <= 1) {
                return 1;
            }
            return n * factorial(n - 1);
        }
        factorial(5);
    """);
    // Assert result is 120
}

@Test
void functionAsArgument() {
    Program program = PeelGrammar.parse("""
        apply = fn(f, x) {
            return f(x);
        }
        double = fn(n) {
            return n * 2;
        }
        apply(double, 5);
    """);
    // Assert result is 10
}
```

## Open Questions

1. **Return value semantics**:
    - Explicit return required or implicit (last expression)?
    - **Recommendation**: Implicit return (last expression), explicit return for early exit

2. **Void/unit type**:
    - What do functions without return value evaluate to?
    - **Recommendation**: Add `Void` or `Unit` primitive type

3. **Closure capture semantics**:
    - Reference (mutable) or copy (immutable)?
    - **Recommendation**: Reference for simplicity

4. **Function equality**:
    - Can you compare two function values?
    - **Recommendation**: Not for MVP, throw error

5. **Anonymous functions**:
    - Support `fn() {}` without assignment?
    - **Answer**: Yes, that's what the grammar supports

6. **Multiple return values**:
    - Support returning multiple values?
    - **Recommendation**: Not for MVP, use list/map

7. **Default parameters**:
    - Support `fn(a, b = 5) {}`?
    - **Recommendation**: Not for MVP

8. **Variadic functions**:
    - Support `fn(args...) {}`?
    - **Recommendation**: Not for MVP

9. **Named parameters**:
    - Support `add(a: 3, b: 5)`?
    - **Recommendation**: Not for MVP

## Definition of Done

- [ ] Grammar supports function definitions and return statements
- [ ] Parser creates FunctionDefinition and ReturnStatement AST nodes
- [ ] FunctionValue type implemented
- [ ] Runtime evaluates function definitions to FunctionValue
- [ ] Runtime calls user-defined functions
- [ ] Parameters are bound correctly in function scope
- [ ] Return statement exits function with value
- [ ] Closures capture outer scope
- [ ] Recursive functions work
- [ ] Functions can be passed as values
- [ ] All test cases pass
- [ ] Built-in functions still work (no regression)
- [ ] EvaluatedExpression captures function calls properly
- [ ] Good error messages for arity mismatch, etc.
- [ ] Documentation updated

## Notes for Next Epic

Epic 5 (Extensibility) can now build on:

- Complete language with functions
- Stable runtime with scoping
- Time to make arithmetic and operators pluggable
- Improve host function registration API
