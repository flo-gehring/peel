# Epic 8: Compiled Programs (Backlog)

**Status**: Backlog - Post-MVP
**Dependencies**: MVP Complete (Epic 1-6)
**Priority**: Medium
**Complexity**: Low-Medium (mostly API design)
**Goal**: Separate parsing from evaluation to enable reusable, pre-compiled programs

## Overview

Currently, every evaluation parses the source code:

```java
// Current: Parse every time
String source = "x + y";
Program program = PeelGrammar.parse(source);  // Parsing
EvaluatedProgram result = runtime.run(program);  // Evaluation
```

**Problem**: If you run the same expression 1000 times with different inputs, you parse 1000 times.

**Solution**: Parse once, evaluate many times:

```java
// Better: Parse once
CompiledProgram compiled = PeelGrammar.compile(source);  // Parse once

// Evaluate many times with different inputs
result1 = runtime.run(compiled, Map.of("x", 1, "y", 2));
result2 = runtime.run(compiled, Map.of("x", 5, "y", 10));
result3 = runtime.run(compiled, Map.of("x", 100, "y", 200));
```

**Benefits**:

- **Performance**: Skip parsing on repeated evaluations
- **Validation**: Syntax errors found once at compile time
- **Reusability**: Same logic, different data
- **Optimization**: Can optimize the compiled form

## Use Cases

### 1. Template Expressions

```java
// E-commerce: Calculate price for many products
CompiledProgram priceFormula = compiler.compile("""
    basePrice * (1 - discount) * (1 + taxRate)
""");

for (Product product : products) {
    Map<String, PeelValue> vars = Map.of(
        "basePrice", PeelValue.integer(product.getPrice()),
        "discount", PeelValue.decimal(product.getDiscount()),
        "taxRate", PeelValue.decimal(0.19)
    );
    EvaluatedProgram result = runtime.run(priceFormula, vars);
    product.setFinalPrice(result.finalValue());
}
```

### 2. Rule Engine

```java
// Evaluate business rule against many records
CompiledProgram rule = compiler.compile("""
    age >= 18 && creditScore > 600 && income > 30000
""");

for (Application app : applications) {
    Map<String, PeelValue> context = Map.of(
        "age", PeelValue.integer(app.getAge()),
        "creditScore", PeelValue.integer(app.getCreditScore()),
        "income", PeelValue.integer(app.getIncome())
    );

    EvaluatedProgram result = runtime.run(rule, context);
    if (result.finalValue().equals(Bool.TRUE)) {
        approveApplication(app);
    }
}
```

### 3. Configuration Expressions

```java
// Load once, evaluate many times in different contexts
CompiledProgram timeout = compiler.compile("""
    if (environment == "production") {
        30;
    } else {
        5;
    }
""");

// Dev environment
int devTimeout = runtime.run(timeout,
    Map.of("environment", PeelValue.text("dev"))
).finalValue();

// Prod environment
int prodTimeout = runtime.run(timeout,
    Map.of("environment", PeelValue.text("production"))
).finalValue();
```

### 4. Validation Rules

```java
// Pre-compile validation rules at startup
Map<String, CompiledProgram> validators = Map.of(
    "email", compiler.compile("email.contains('@') && email.length() > 3"),
    "age", compiler.compile("age >= 0 && age <= 150"),
    "username", compiler.compile("username.length() >= 3 && username.length() <= 20")
);

// Validate user input at runtime (no parsing overhead)
public boolean validateEmail(String email) {
    return runtime.run(validators.get("email"),
        Map.of("email", PeelValue.text(email))
    ).finalValue().equals(Bool.TRUE);
}
```

## Current State Analysis

Looking at the existing code:

**Good news**: `Program` is already immutable (it's a record):

```java
public record Program(List<CodeElement> codeElement) {}
```

**Problem 1**: `SimpleRuntime` has mutable state:

```java
private final HashMap<String, EvaluatedExpression> variables;  // Mutates!
private final HashMap<String, List<Function>> functions;       // Mutates!
```

Variables registered or created during one evaluation persist to the next.

**Problem 2**: No way to pass initial variables:

```java
// Current: Can only use registered variables
runtime.register(new Variable("x", PeelValue.integer(5)));
runtime.run(program);
```

**Problem 3**: Terminology confusion:

- `Program` is already the "compiled" form (AST)
- Need better naming to distinguish source → AST → evaluation

## Features

### 1. Immutable Programs (Already Done!)

- **Description**: `Program` is already immutable and reusable
- **No changes needed**: Current `Program` record works
- **Validation**: Ensure runtime doesn't mutate programs

### 2. Stateless Evaluation

- **Description**: Runtime doesn't carry state between evaluations
- **API Change**:
  ```java
  interface Runtime {
      // Old: Stateful
      void register(Variable v);
      EvaluatedProgram run(Program program);

      // New: Stateless
      EvaluatedProgram run(Program program, Map<String, PeelValue> initialVariables);
      EvaluatedProgram run(Program program, ExecutionContext context);
  }
  ```

### 3. Execution Context

- **Description**: Encapsulate runtime state for one evaluation
- **Design**:
  ```java
  class ExecutionContext {
      private final Map<String, PeelValue> initialVariables;
      private final Map<String, Function> functions;
      private final ArithmeticEngine arithmetic;

      public static ExecutionContextBuilder builder() { ... }
  }
  ```

### 4. Compile-Time Validation (Optional)

- **Description**: Validate program at compile time
- **Checks**:
    - Syntax errors (already done by parser)
    - Undefined variables (optional - could warn)
    - Type errors (optional - runtime checks are fine)
- **Recommendation**: Skip for now, defer to static analysis epic

### 5. Serialization (Optional)

- **Description**: Serialize `Program` to bytes for caching
- **Use case**: Load pre-compiled programs from disk
- **Implementation**: Java serialization or custom format
- **Recommendation**: Nice-to-have, not critical

### 6. Optimization Pass (Optional)

- **Description**: Optimize AST after parsing
- **Examples**:
    - Constant folding: `1 + 2` → `3`
    - Dead code elimination
    - Common subexpression elimination
- **Recommendation**: Defer to performance epic (backlog idea)

## Technical Implementation

### Terminology Clarification

**Current**:

```
String source code
    ↓ (parse)
Program (AST)
    ↓ (run)
EvaluatedProgram (result)
```

**Naming convention**: `Program` is fine - it's the compiled form.

### ExecutionContext Design

```java
public class ExecutionContext {
    private final Map<String, PeelValue> initialVariables;
    private final Map<String, Function> registeredFunctions;
    private final ArithmeticEngine arithmetic;
    private final FunctionRegistry functionRegistry;

    // Package-private constructor
    ExecutionContext(
        Map<String, PeelValue> initialVariables,
        Map<String, Function> registeredFunctions,
        ArithmeticEngine arithmetic,
        FunctionRegistry functionRegistry
    ) {
        this.initialVariables = Map.copyOf(initialVariables);  // Defensive copy
        this.registeredFunctions = Map.copyOf(registeredFunctions);
        this.arithmetic = arithmetic;
        this.functionRegistry = functionRegistry;
    }

    public static ExecutionContextBuilder builder() {
        return new ExecutionContextBuilder();
    }

    public static ExecutionContext empty() {
        return builder().build();
    }

    public static ExecutionContext withVariables(Map<String, PeelValue> vars) {
        return builder().initialVariables(vars).build();
    }

    // Getters
    public Map<String, PeelValue> getInitialVariables() {
        return initialVariables;
    }

    // ... other getters
}

public class ExecutionContextBuilder {
    private Map<String, PeelValue> initialVariables = new HashMap<>();
    private Map<String, Function> functions = new HashMap<>();
    private ArithmeticEngine arithmetic = new StandardArithmetic();
    private FunctionRegistry functionRegistry = new DefaultFunctionRegistry();

    public ExecutionContextBuilder initialVariables(Map<String, PeelValue> vars) {
        this.initialVariables.putAll(vars);
        return this;
    }

    public ExecutionContextBuilder variable(String name, PeelValue value) {
        this.initialVariables.put(name, value);
        return this;
    }

    public ExecutionContextBuilder function(String name, Function func) {
        this.functions.put(name, func);
        return this;
    }

    public ExecutionContextBuilder arithmetic(ArithmeticEngine engine) {
        this.arithmetic = engine;
        return this;
    }

    public ExecutionContext build() {
        return new ExecutionContext(
            initialVariables,
            functions,
            arithmetic,
            functionRegistry
        );
    }
}
```

### Runtime API Changes

```java
public interface Runtime {
    // Old API (deprecated but kept for compatibility)
    @Deprecated
    void register(Variable v);

    @Deprecated
    void register(Function f);

    @Deprecated
    EvaluatedProgram run(Program program);

    // New API: Stateless execution
    EvaluatedProgram run(Program program, ExecutionContext context);

    // Convenience methods
    default EvaluatedProgram run(Program program, Map<String, PeelValue> variables) {
        ExecutionContext context = ExecutionContext.withVariables(variables);
        return run(program, context);
    }

    default EvaluatedProgram run(Program program) {
        return run(program, ExecutionContext.empty());
    }
}
```

### SimpleRuntime Refactoring

**Before**:

```java
public class SimpleRuntime implements Runtime {
    private final HashMap<String, EvaluatedExpression> variables;  // Mutable!
    private final HashMap<String, List<Function>> functions;       // Mutable!

    @Override
    public EvaluatedProgram run(Program program) {
        // Uses instance variables
    }
}
```

**After**:

```java
public class SimpleRuntime implements Runtime {
    // Remove instance state - make stateless
    // OR keep for backward compatibility with deprecated API

    @Override
    public EvaluatedProgram run(Program program, ExecutionContext context) {
        // Create fresh scope from context
        Scope globalScope = new Scope(null);

        // Populate with initial variables
        for (Map.Entry<String, PeelValue> entry : context.getInitialVariables().entrySet()) {
            globalScope.declare(entry.getKey(),
                new EvaluatedExpression.Literal(entry.getValue()));
        }

        // Create executor with context
        Executor executor = new DefaultExecutor(
            globalScope,
            context.getArithmetic(),
            context.getFunctionRegistry()
        );

        // Execute program
        return executor.execute(program);
    }
}
```

### Parser API Enhancement

```java
public class PeelGrammar {
    // Existing method (unchanged)
    public static Program parse(String source) {
        // ... existing implementation
    }

    // Alias for clarity
    public static Program compile(String source) {
        return parse(source);  // Same thing, clearer intent
    }

    // Future: Compile with validation
    public static CompiledProgram compileWithValidation(String source) {
        Program program = parse(source);
        // Perform static analysis
        // Return wrapped program with metadata
    }
}
```

## Test-Driven Development

### Test Order

1. **ExecutionContextTest**: Test context builder
2. **StatelessRuntimeTest**: Test multiple runs don't interfere
3. **ParameterizedExecutionTest**: Test passing different variables
4. **PerformanceTest**: Verify parsing overhead eliminated
5. **BackwardCompatibilityTest**: Old API still works
6. **ConcurrencyTest**: Same program, concurrent executions

### Example Test Cases

```java
@Test
void programCanBeReusedWithDifferentVariables() {
    Program program = PeelGrammar.compile("x + y");

    EvaluatedProgram result1 = runtime.run(program,
        Map.of(
            "x", PeelValue.integer(1),
            "y", PeelValue.integer(2)
        )
    );
    assertEquals(3, result1.finalValue());

    EvaluatedProgram result2 = runtime.run(program,
        Map.of(
            "x", PeelValue.integer(10),
            "y", PeelValue.integer(20)
        )
    );
    assertEquals(30, result2.finalValue());
}

@Test
void executionsAreIsolated() {
    Program program = PeelGrammar.compile("""
        a = 5;
        b = 10;
        a + b;
    """);

    runtime.run(program, Map.of());
    runtime.run(program, Map.of());

    // Variables from first run should not leak to second run
    // Both should succeed
}

@Test
void contextBuilderAPI() {
    Program program = PeelGrammar.compile("x * multiplier");

    ExecutionContext context = ExecutionContext.builder()
        .variable("x", PeelValue.integer(5))
        .variable("multiplier", PeelValue.integer(3))
        .arithmetic(new StandardArithmetic())
        .build();

    EvaluatedProgram result = runtime.run(program, context);
    assertEquals(15, result.finalValue());
}

@Test
void parseOnceEvaluateMany_Performance() {
    String source = "a + b * c - d / e";

    // Measure: Parse + evaluate 1000 times
    long startParseEach = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
        Program p = PeelGrammar.parse(source);
        runtime.run(p, Map.of(
            "a", PeelValue.integer(1),
            "b", PeelValue.integer(2),
            "c", PeelValue.integer(3),
            "d", PeelValue.integer(4),
            "e", PeelValue.integer(5)
        ));
    }
    long timeParseEach = System.nanoTime() - startParseEach;

    // Measure: Parse once, evaluate 1000 times
    long startParseOnce = System.nanoTime();
    Program compiled = PeelGrammar.compile(source);
    for (int i = 0; i < 1000; i++) {
        runtime.run(compiled, Map.of(
            "a", PeelValue.integer(1),
            "b", PeelValue.integer(2),
            "c", PeelValue.integer(3),
            "d", PeelValue.integer(4),
            "e", PeelValue.integer(5)
        ));
    }
    long timeParseOnce = System.nanoTime() - startParseOnce;

    // Parse-once should be significantly faster
    assertThat(timeParseOnce).isLessThan(timeParseEach / 2);
}

@Test
void concurrentExecutionsSafe() throws InterruptedException {
    Program program = PeelGrammar.compile("x + y");

    // Run same program concurrently with different inputs
    List<Thread> threads = new ArrayList<>();
    List<Integer> results = new CopyOnWriteArrayList<>();

    for (int i = 0; i < 100; i++) {
        final int value = i;
        Thread t = new Thread(() -> {
            EvaluatedProgram result = runtime.run(program,
                Map.of(
                    "x", PeelValue.integer(value),
                    "y", PeelValue.integer(value)
                )
            );
            results.add(((Number.Integer) result.finalValue()).value());
        });
        threads.add(t);
        t.start();
    }

    for (Thread t : threads) {
        t.join();
    }

    // All results should be correct
    assertEquals(100, results.size());
    for (int i = 0; i < 100; i++) {
        assertTrue(results.contains(i + i));
    }
}

@Test
void backwardCompatibility_OldAPIStillWorks() {
    Runtime runtime = SimpleRuntime.empty();
    runtime.register(new Variable("x", PeelValue.integer(5)));

    Program program = PeelGrammar.parse("x + 3");
    EvaluatedProgram result = runtime.run(program);

    assertEquals(8, result.finalValue());
}
```

## Open Questions

1. **Naming**:
    - Is `Program` a good name for the compiled form?
    - Alternative: `CompiledProgram`, `ParsedProgram`, `AST`
    - **Recommendation**: Keep `Program`, it's fine

2. **Serialization format**:
    - Java serialization (simple but fragile)?
    - Custom binary format (compact but complex)?
    - JSON (human-readable but verbose)?
    - **Recommendation**: Defer to separate epic if needed

3. **Validation scope**:
    - Just syntax (already done)?
    - Undefined variables (warn or error)?
    - Type checking (defer to type system)?
    - **Recommendation**: Just syntax for now

4. **Caching**:
    - Should runtime cache compiled programs by source?
    - Memory implications?
    - **Recommendation**: Let users cache if they want

5. **Optimization**:
    - Constant folding at compile time?
    - Dead code elimination?
    - **Recommendation**: Defer to performance epic

6. **Backward compatibility**:
    - Keep old stateful API?
    - Migration path?
    - **Recommendation**: Deprecate but keep for one version

## Definition of Done

- [ ] `ExecutionContext` class implemented
- [ ] `ExecutionContextBuilder` implemented
- [ ] `Runtime.run(Program, ExecutionContext)` method added
- [ ] `Runtime.run(Program, Map<String, PeelValue>)` convenience method
- [ ] `SimpleRuntime` refactored to be stateless
- [ ] Old API deprecated but functional
- [ ] `PeelGrammar.compile()` alias added
- [ ] All test cases pass
- [ ] Performance improvement measured and documented
- [ ] Concurrent execution safe
- [ ] Documentation updated with examples
- [ ] Migration guide for users

## Benefits Summary

**Performance**:

- Skip parsing on repeated evaluations
- For 1000 evaluations: ~50-90% time savings (depends on expression complexity)

**API Quality**:

- Clearer intent: `compile()` vs `run()`
- More flexible: Different contexts for same program
- Thread-safe: No shared mutable state

**Use Cases Enabled**:

- Template expressions
- Rule engines
- Configuration evaluation
- Validation rules
- Batch processing

**Complexity**: Low-Medium

- Mostly API design and refactoring
- Core evaluation logic unchanged
- Biggest change: Making runtime stateless

**Recommendation**: High value, low-medium effort. Good early post-MVP feature.
