# Epic 5: Extensibility

**Status**: Not Started
**Dependencies**: Epic 1-4 (Complete language implementation)
**Goal**: Redesign runtime architecture to make arithmetic and evaluation strategy pluggable while keeping a default
executor.

## Overview

This epic addresses the concern from README:
> "I don't like the runtime concept i have right now. The interface is fine but users should not be forced to implement
> their own evaluation, scoping logic etc if they don't like the arithmetic etc."

**Problem**: Currently, to customize arithmetic or operators, users must implement the entire `Runtime` interface and
handle evaluation, scoping, etc.

**Solution**: Separate concerns:

1. **Executor**: Handles evaluation, scoping, control flow (provided by PEEL)
2. **Arithmetic Engine**: Handles number operations (pluggable)
3. **Function Registry**: Handles built-in functions (pluggable)

Users should be able to replace #2-3 without reimplementing #1.

## Features

### 1. Pluggable Arithmetic

- **Description**: Allow users to customize how arithmetic works
- **Use cases**:
    - Different number types (BigDecimal, rationals, complex)
    - Units (5m + 3m = 8m, but 5m + 3s = error)
    - Modular arithmetic
    - Saturating arithmetic
- **API**:
  ```java
  interface ArithmeticEngine {
      PeelValue add(PeelValue lhs, PeelValue rhs);
      PeelValue subtract(PeelValue lhs, PeelValue rhs);
      PeelValue multiply(PeelValue lhs, PeelValue rhs);
      PeelValue divide(PeelValue lhs, PeelValue rhs);
      // ... other operations
  }
  ```

### 2. Enhanced Function Registration

- **Description**: Improve built-in function registration API
- **Current pain points**:
    - Overloading by arity only (not by type)
    - Function interface is simple but verbose to implement
    - No type checking/validation
- **Improvements**:
  ```java
  runtime.registerFunction("add", 2,
      (a, b) -> {
          // Type checking helpers
          Number numA = requireNumber(a, "add");
          Number numB = requireNumber(b, "add");
          return add(numA, numB);
      }
  );

  // Or with type-safe builder
  runtime.register()
      .function("add")
      .parameters(Number.class, Number.class)
      .implementation((a, b) -> a.add(b))
      .build();
  ```

### 3. Default Executor with Plugins

- **Description**: Users get default evaluation but can customize parts
- **Architecture**:
  ```java
  class ConfigurableRuntime implements Runtime {
      private final Executor executor;  // handles evaluation, scoping
      private final ArithmeticEngine arithmetic;
      private final FunctionRegistry functions;

      ConfigurableRuntime(ArithmeticEngine arithmetic, ...) {
          this.executor = new DefaultExecutor();  // provided by PEEL
          this.arithmetic = arithmetic;
          // ...
      }
  }
  ```

### 4. Builder/Factory API

- **Description**: Convenient API for configuring runtime
- **Example usage**:
  ```java
  Runtime runtime = RuntimeBuilder.create()
      .withArithmetic(new DecimalArithmetic())
      .withStandardFunctions()
      .registerFunction("customFunc", ...)
      .build();
  ```

## Technical Implementation

### Architecture Refactoring

**Current**:

```
Runtime (interface)
  └─ SimpleRuntime (implements everything)
```

**New**:

```
Runtime (interface)
  └─ ConfigurableRuntime
       ├─ Executor (evaluation, scoping)
       ├─ ArithmeticEngine (pluggable)
       └─ FunctionRegistry (pluggable)
```

### Executor Interface

Extract evaluation logic into separate component:

```java
interface Executor {
    EvaluatedProgram execute(Program program, ExecutionContext context);
    EvaluatedExpression evaluateExpression(Expression expr, ExecutionContext context);
    EvaluatedStatement executeStatement(Statement stmt, ExecutionContext context);
}

class ExecutionContext {
    private Scope currentScope;
    private final ArithmeticEngine arithmetic;
    private final FunctionRegistry functions;

    // ... methods for variable lookup, function calls, etc.
}
```

### ArithmeticEngine Implementation

```java
interface ArithmeticEngine {
    PeelValue add(PeelValue lhs, PeelValue rhs);
    PeelValue subtract(PeelValue lhs, PeelValue rhs);
    PeelValue multiply(PeelValue lhs, PeelValue rhs);
    PeelValue divide(PeelValue lhs, PeelValue rhs);
    PeelValue negate(PeelValue value);
    // ... other operations
}

// Default implementation
class StandardArithmetic implements ArithmeticEngine {
    // Current logic from RuntimeFactory
}

// Example custom implementation
class UnitAwareArithmetic implements ArithmeticEngine {
    @Override
    public PeelValue add(PeelValue lhs, PeelValue rhs) {
        // Check units match, add values
    }
}
```

### FunctionRegistry Enhancement

```java
interface FunctionRegistry {
    void register(String name, int arity, Function implementation);
    void register(String name, Function implementation);  // overload by type signature
    Optional<Function> resolve(String name, List<PeelValue> arguments);
}

class TypedFunctionRegistry implements FunctionRegistry {
    // Support type-based overloading
    record Signature(String name, List<Class<? extends PeelValue>> paramTypes) {}

    private final Map<Signature, Function> functions = new HashMap<>();

    @Override
    public Optional<Function> resolve(String name, List<PeelValue> arguments) {
        // Try exact type match first
        // Fall back to arity match
        // Error if ambiguous
    }
}
```

### Backward Compatibility

Keep `SimpleRuntime` as convenience wrapper:

```java
public class SimpleRuntime implements Runtime {
    private final ConfigurableRuntime delegate;

    public static SimpleRuntime empty() {
        return new SimpleRuntime(RuntimeBuilder.create().build());
    }

    // Delegate all methods
}
```

## Test-Driven Development

### Refactoring Strategy

1. **Extract Executor** from SimpleRuntime (tests should still pass)
2. **Extract ArithmeticEngine** (tests should still pass)
3. **Add FunctionRegistry** (tests should still pass)
4. **Build ConfigurableRuntime** (tests should still pass)
5. **Add builder API** and test customization
6. **Write integration tests** for custom engines

### Test Order

1. **ExecutorTest**: Test executor with standard plugins
2. **ArithmeticEngineTest**: Test custom arithmetic
3. **FunctionRegistryTest**: Test function registration and overloading
4. **ConfigurableRuntimeTest**: Test runtime with custom plugins
5. **RuntimeBuilderTest**: Test builder API
6. **RegressionTest**: All Epic 1-4 tests still pass
7. **IntegrationTest**: Example custom configurations

### Example Test Cases

```java
@Test
void customArithmetic() {
    Runtime runtime = RuntimeBuilder.create()
        .withArithmetic(new ModularArithmetic(100))
        .build();

    Program program = PeelGrammar.parse("99 + 5;");
    EvaluatedProgram result = runtime.run(program);
    // Assert result is 4 (99 + 5 mod 100)
}

@Test
void typeBasedFunctionOverload() {
    Runtime runtime = RuntimeBuilder.create()
        .withStandardArithmetic()
        .registerFunction("format", Text.class, t -> formatText(t))
        .registerFunction("format", Number.class, n -> formatNumber(n))
        .build();

    // Test both overloads
}
```

## Open Questions

1. **Operator Registry**:
    - Should users be able to register custom operators (new symbols, precedence)?
    - Are operators just infix functions? Should they use function registry?
    - Can grammar be made dynamic for operator precedence?
    - **Options**:
        - Fixed grammar, only allow overriding operator implementation
        - Generate grammar dynamically (complex)
        - Parse operators generically and apply precedence in AST building
    - **Needs decision**: Scope of operator customization for this epic

2. **Type system**:
    - Do we need formal type system for function overloading?
    - **Recommendation**: Not for MVP, use runtime type checking

3. **Primitive types**:
    - Can users add new primitive types (e.g., units)?
    - **Problem**: PeelValue is sealed and can't be extended outside package
    - **Options**:
        - Make PeelValue non-sealed
        - Add Extension/Custom variant that wraps user types
        - Keep sealed, users work within existing types
    - **Needs decision**: How extensible should the type system be?

4. **Performance**:
    - Does pluggability hurt performance?
    - **Recommendation**: Measure later, correctness first

5. **Error handling**:
    - How do custom engines report errors?
    - **Recommendation**: Throw PeelException with good messages

## Definition of Done

- [ ] Executor extracted and tested
- [ ] ArithmeticEngine interface and standard implementation
- [ ] FunctionRegistry with type-based overloading
- [ ] ConfigurableRuntime combining all plugins
- [ ] RuntimeBuilder API for configuration
- [ ] SimpleRuntime updated to use new architecture
- [ ] All Epic 1-4 tests still pass
- [ ] Custom arithmetic example works
- [ ] Custom function registration works
- [ ] Documentation updated with architecture
- [ ] Migration guide for users of old SimpleRuntime

## Notes for Next Epic

Epic 6 (Output Formats) can leverage:

- Clean separation of concerns
- EvaluatedExpression tree captures everything
- Ready to generate JSON, HTML, markdown, etc.
