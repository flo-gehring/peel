# Epic 7: External Objects (Backlog)

**Status**: Backlog - Post-MVP
**Dependencies**: MVP Complete (Epic 1-6)
**Priority**: Medium-High (if host integration needed)
**Goal**: Allow host applications to safely expose Java objects to PEEL with explicit whitelisting

⚠️ **Security-Critical Epic**: Code injection risks require careful design and security review before implementation.

## Overview

Enable Java host applications to register specific objects that PEEL code can interact with. This solves the "
integration" problem where PEEL needs to access host application data, configuration, or services.

**What this epic IS**:

- Controlled exposure of specific Java objects to PEEL
- Whitelist-based security model (opt-in access only)
- Method calls and field access from PEEL
- Host integration for embedded expression language use cases

**What this epic IS NOT**:

- Full OOP in PEEL (no class definitions, inheritance, constructors)
- Arbitrary Java access (no reflection, no dangerous types)
- General-purpose object system (maps + functions cover that)

## Security Model

**Fundamental principle**: Default deny, explicit allow.

Every object registered with PEEL must:

1. Be explicitly registered by host code (not accessible via PEEL)
2. Have an explicit whitelist of accessible methods
3. Have an explicit whitelist of accessible fields
4. Be wrapped in a security proxy that enforces whitelist

**Never allowed**:

- `getClass()`, reflection APIs
- `Runtime`, `System`, `ClassLoader`, `Process`
- File I/O classes unless explicitly required and understood
- Serialization/deserialization
- Network access unless explicitly required and understood
- Thread creation

## Use Cases

### 1. Application Configuration

```java
// Host Java code
AppConfig config = new AppConfig();
config.

setTimeout(30);
config.

setMaxRetries(3);

runtime.

registerObject("config")
    .

object(config)
    .

allowMethod("getTimeout")
    .

allowMethod("getMaxRetries")
    .

allowField("version")  // read-only
    .

build();

// PEEL code
timeout =config.

getTimeout();
if(timeout >60){
        // ... handle long timeout
        }
```

### 2. Database Query Results

```java
// Host Java code
ResultSet results = database.query("SELECT * FROM users");

runtime.

registerObject("results")
    .

object(results)
    .

allowMethod("next")
    .

allowMethod("getString",String .class)
    .

allowMethod("getInt",String .class)
    .

build();

// PEEL code
while(results.

next()){
name =results.

getString("name");

age =results.

getInt("age");
// ... process
}
```

### 3. Custom Business Logic

```java
// Host Java code
PriceCalculator calculator = new PriceCalculator();

runtime.

registerObject("pricing")
    .

object(calculator)
    .

allowMethod("calculateDiscount",Number .class, Number .class)
    .

allowMethod("applyTax",Number .class)
    .

build();

// PEEL code
basePrice =100;
discount =pricing.

calculateDiscount(basePrice, customerTier);

finalPrice =pricing.

applyTax(basePrice -discount);
```

## Features

### 1. Object Registration API

- **Description**: Host API to register Java objects with whitelisting
- **Requirements**:
    - Builder pattern for clear configuration
    - Type-safe method signatures
    - Read-only vs read-write field access
    - Clear error messages for misconfiguration
- **Test cases**:
    - Register object with methods
    - Register object with fields
    - Attempt to call non-whitelisted method (should error)
    - Attempt to access non-whitelisted field (should error)

### 2. Method Invocation

- **Description**: Call whitelisted methods from PEEL
- **Syntax**: `object.methodName(args)`
- **Requirements**:
    - Type checking of arguments
    - Return value conversion to PeelValue
    - Error handling for Java exceptions
    - Arity and type matching
- **Test cases**:
    - Call method with correct arguments
    - Call method with wrong argument types
    - Call method with wrong arity
    - Handle Java exceptions gracefully

### 3. Field Access

- **Description**: Read whitelisted fields from PEEL
- **Syntax**: `object.fieldName`
- **Requirements**:
    - Read-only by default
    - Optional read-write for specific fields
    - Automatic conversion to PeelValue
- **Test cases**:
    - Read field value
    - Write to read-write field
    - Attempt write to read-only field (should error)

### 4. Type Conversion

- **Description**: Convert between Java types and PeelValue
- **Mappings**:
    - Java primitives ↔ PEEL Number/Bool/Text
    - Java String ↔ PEEL Text
    - Java collections ↔ PEEL List/Map
    - Java objects ↔ PEEL ExternalObject
- **Edge cases**: Null handling, unsupported types

### 5. Security Enforcement

- **Description**: Runtime checks prevent unauthorized access
- **Requirements**:
    - Wrapper proxy intercepts all calls
    - Reflection blocked entirely
    - Dangerous method names blacklisted
    - Security audit logging (optional)
- **Test cases**:
    - Attempt reflection (should error)
    - Attempt dangerous method calls (should error)
    - Verify proxy cannot be bypassed

## Technical Implementation

### New PeelValue Type

```java
// In core.values
record ExternalObject(
                String name,           // Registered name
                Object javaObject,     // Wrapped Java object
                ObjectDescriptor descriptor  // Whitelist configuration
        ) implements PeelValue {
    // Prevent direct construction
    // Only created via registration API
}

record ObjectDescriptor(
        Map<MethodSignature, MethodHandle> allowedMethods,
        Map<String, FieldDescriptor> allowedFields
) {
    boolean isMethodAllowed(String name, List<Class<?>> paramTypes) { ...}

    boolean isFieldAllowed(String name) { ...}

    boolean isFieldWritable(String name) { ...}
}

record MethodSignature(String name, List<Class<?>> paramTypes) {
}

record FieldDescriptor(String name, boolean writable) {
}
```

### Registration API

```java
// In Runtime or new ObjectRegistry
interface ObjectRegistry {
    ObjectRegistrationBuilder registerObject(String name);
}

class ObjectRegistrationBuilder {
    private Object object;
    private Set<MethodSignature> methods = new HashSet<>();
    private Set<FieldDescriptor> fields = new HashSet<>();

    public ObjectRegistrationBuilder object(Object obj) {
        SecurityValidator.validateNotDangerous(obj);
        this.object = obj;
        return this;
    }

    public ObjectRegistrationBuilder allowMethod(String name, Class<?>... paramTypes) {
        SecurityValidator.validateMethodSafe(object.getClass(), name, paramTypes);
        methods.add(new MethodSignature(name, List.of(paramTypes)));
        return this;
    }

    public ObjectRegistrationBuilder allowField(String name) {
        return allowField(name, false);  // read-only by default
    }

    public ObjectRegistrationBuilder allowField(String name, boolean writable) {
        SecurityValidator.validateFieldSafe(object.getClass(), name);
        fields.add(new FieldDescriptor(name, writable));
        return this;
    }

    public void build() {
        ObjectDescriptor descriptor = new ObjectDescriptor(
                buildMethodHandles(),
                fields.stream().collect(Collectors.toMap(
                        FieldDescriptor::name,
                        f -> f
                ))
        );
        ExternalObject external = new ExternalObject(name, object, descriptor);
        runtime.registerVariable(name, external);
    }

    private Map<MethodSignature, MethodHandle> buildMethodHandles() {
        // Use MethodHandles for performance and safety
        // Validate each method exists and is accessible
    }
}
```

### Security Validator

```java
class SecurityValidator {
    private static final Set<String> BLACKLISTED_METHODS = Set.of(
            "getClass", "notify", "notifyAll", "wait",
            "clone", "finalize"  // Object methods
    );

    private static final Set<Class<?>> DANGEROUS_TYPES = Set.of(
            Runtime.class, Process.class, ProcessBuilder.class,
            ClassLoader.class, Class.class,
            System.class, Thread.class,
            java.io.File.class, java.io.FileWriter.class,
            java.lang.reflect.Method.class,
            // ... more dangerous types
    );

    public static void validateNotDangerous(Object obj) {
        Class<?> clazz = obj.getClass();
        for (Class<?> dangerous : DANGEROUS_TYPES) {
            if (dangerous.isAssignableFrom(clazz)) {
                throw new SecurityException(
                        "Cannot register dangerous type: " + clazz.getName()
                );
            }
        }
    }

    public static void validateMethodSafe(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        if (BLACKLISTED_METHODS.contains(methodName)) {
            throw new SecurityException(
                    "Method blacklisted: " + methodName
            );
        }

        try {
            java.lang.reflect.Method method = clazz.getMethod(methodName, paramTypes);
            // Validate return type is convertible to PeelValue
            // Validate parameters are convertible from PeelValue
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Method not found: " + methodName
            );
        }
    }

    public static void validateFieldSafe(Class<?> clazz, String fieldName) {
        // Similar validation for fields
    }
}
```

### Grammar Changes

```antlr
expr
    : expr '.' IDENT                    # fieldAccessExpr
    | expr '.' IDENT '(' exprList? ')'  # methodCallExpr
    | ...
    ;
```

### AST Changes

```java
// In Expression
record FieldAccess(Expression object, String fieldName) implements Expression {
}

record MethodCall(Expression object, String methodName, List<Expression> arguments)
        implements Expression {
}
```

**Note**: `Expression.FunctionCall` already exists but assumes function name is String.
May need to refactor to unify with method calls, or keep separate.

### Runtime Evaluation

```java
// In Executor
private EvaluatedExpression evaluateFieldAccess(Expression.FieldAccess access) {
    EvaluatedExpression obj = evaluateExpr(access.object());

    if (obj.result() instanceof ExternalObject external) {
        ObjectDescriptor desc = external.descriptor();
        if (!desc.isFieldAllowed(access.fieldName())) {
            throw new PeelException(
                    "Field not whitelisted: " + access.fieldName()
            );
        }

        try {
            Object value = getFieldValue(external.javaObject(), access.fieldName());
            PeelValue peelValue = JavaTypeConverter.toPeelValue(value);
            return new EvaluatedExpression.FieldAccess(
                    obj,
                    access.fieldName(),
                    peelValue
            );
        } catch (Exception e) {
            throw new PeelException("Field access failed", e);
        }
    } else {
        throw new PeelException(
                "Field access on non-object: " + obj.result()
        );
    }
}

private EvaluatedExpression evaluateMethodCall(Expression.MethodCall call) {
    EvaluatedExpression obj = evaluateExpr(call.object());

    if (obj.result() instanceof ExternalObject external) {
        List<EvaluatedExpression> args = call.arguments().stream()
                .map(this::evaluateExpr)
                .toList();

        MethodSignature sig = new MethodSignature(
                call.methodName(),
                args.stream().map(a -> a.result().getClass()).toList()
        );

        MethodHandle handle = external.descriptor()
                .allowedMethods()
                .get(sig);

        if (handle == null) {
            throw new PeelException(
                    "Method not whitelisted: " + call.methodName()
            );
        }

        try {
            Object[] javaArgs = JavaTypeConverter.toJavaArray(args);
            Object result = handle.invokeWithArguments(external.javaObject(), javaArgs);
            PeelValue peelValue = JavaTypeConverter.toPeelValue(result);

            return new EvaluatedExpression.MethodCall(
                    obj,
                    call.methodName(),
                    args,
                    peelValue
            );
        } catch (Throwable e) {
            throw new PeelException("Method invocation failed", e);
        }
    } else {
        throw new PeelException(
                "Method call on non-object: " + obj.result()
        );
    }
}
```

### Type Conversion

```java
class JavaTypeConverter {
    public static PeelValue toPeelValue(Object javaValue) {
        if (javaValue == null) {
            // Decision needed: Null handling
            throw new PeelException("Null not supported");
        }

        return switch (javaValue) {
            case Integer i -> new Number.Integer(i);
            case Long l -> new Number.Integer(l.intValue());  // or BigDecimal?
            case Double d -> new Number.Float(d);
            case Float f -> new Number.Float(f);
            case Boolean b -> new Bool(b);
            case String s -> new Text(s);
            case java.util.List<?> list -> convertList(list);
            case java.util.Map<?, ?> map -> convertMap(map);
            default -> throw new PeelException(
                    "Unsupported Java type: " + javaValue.getClass()
            );
        };
    }

    public static Object toJavaValue(PeelValue peelValue) {
        return switch (peelValue) {
            case Number.Integer(var i) -> i;
            case Number.Float(var f) -> f;
            case Number.Decimal(var d) -> d;
            case Bool(var b) -> b;
            case Text(var t) -> t;
            case PeelValue.Collection.List(var list) -> list.stream().map(JavaTypeConverter::toJavaValue).toList();
            case PeelValue.Collection.Map(var map) -> convertPeelMapToJava(map);
            case ExternalObject(var name, var obj, var desc) -> obj;  // unwrap
            default -> throw new PeelException(
                    "Cannot convert to Java: " + peelValue
            );
        };
    }

    public static Object[] toJavaArray(List<EvaluatedExpression> args) {
        return args.stream()
                .map(EvaluatedExpression::result)
                .map(JavaTypeConverter::toJavaValue)
                .toArray();
    }
}
```

## Test-Driven Development

### Test Order

1. **SecurityValidatorTest**: Test dangerous type detection
2. **ObjectRegistrationTest**: Test registration API
3. **FieldAccessTest**: Test field reading/writing
4. **MethodCallTest**: Test method invocation
5. **TypeConversionTest**: Test Java ↔ PEEL conversion
6. **SecurityEnforcementTest**: Test whitelist enforcement
7. **IntegrationTest**: Full scenarios with real objects
8. **NegativeSecurityTest**: Attempt exploits (should all fail)

### Example Test Cases

```java

@Test
void registerObjectWithMethods() {
    TestObject obj = new TestObject();
    runtime.registerObject("test")
            .object(obj)
            .allowMethod("getValue")
            .allowMethod("add", Integer.class, Integer.class)
            .build();

    Program program = PeelGrammar.parse("""
                result = test.add(3, 5);
                result;
            """);

    EvaluatedProgram result = runtime.run(program);
    // Assert result is 8
}

@Test
void fieldAccessReadOnly() {
    TestObject obj = new TestObject();
    obj.setValue(42);

    runtime.registerObject("test")
            .object(obj)
            .allowField("value")  // read-only
            .build();

    Program program = PeelGrammar.parse("test.value;");
    EvaluatedProgram result = runtime.run(program);
    // Assert result is 42
}

@Test
void blockNonWhitelistedMethod() {
    TestObject obj = new TestObject();
    runtime.registerObject("test")
            .object(obj)
            .allowMethod("getValue")
            // secretMethod NOT whitelisted
            .build();

    Program program = PeelGrammar.parse("test.secretMethod();");
    assertThrows(PeelException.class, () -> runtime.run(program));
}

@Test
void blockDangerousType() {
    Runtime javaRuntime = Runtime.getRuntime();

    assertThrows(SecurityException.class, () -> {
        runtime.registerObject("dangerous")
                .object(javaRuntime)
                .build();
    });
}

@Test
void blockReflectionAccess() {
    TestObject obj = new TestObject();
    runtime.registerObject("test")
            .object(obj)
            .allowMethod("getValue")
            .build();

    Program program = PeelGrammar.parse("test.getClass();");
    assertThrows(PeelException.class, () -> runtime.run(program));
}

@Test
void typeConversionJavaToPeel() {
    Map<String, Object> javaMap = new HashMap<>();
    javaMap.put("name", "Alice");
    javaMap.put("age", 30);

    PeelValue converted = JavaTypeConverter.toPeelValue(javaMap);
    // Assert converted is PeelValue.Collection.Map
    // Assert contents match
}

@Test
void handleJavaException() {
    FailingObject obj = new FailingObject();
    runtime.registerObject("failing")
            .object(obj)
            .allowMethod("throwException")
            .build();

    Program program = PeelGrammar.parse("failing.throwException();");

    PeelException ex = assertThrows(PeelException.class,
            () -> runtime.run(program));
    assertThat(ex.getMessage()).contains("Method invocation failed");
    assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
}
```

### Security Attack Tests

```java

@Test
void preventReflectionViaToString() {
    TestObject obj = new TestObject();
    runtime.registerObject("test")
            .object(obj)
            .allowMethod("toString")
            .build();

    // Even if toString is allowed, shouldn't expose class info
    Program program = PeelGrammar.parse("test.toString();");
    String result = runtime.run(program).toString();

    // Result should not contain class name or memory address
    assertThat(result).doesNotContain("@");
    assertThat(result).doesNotContain("TestObject");
}

@Test
void preventProcessExecution() {
    // Attempt various ways to get Runtime
    assertThrows(SecurityException.class, () -> {
        runtime.registerObject("rt")
                .object(Runtime.getRuntime())
                .build();
    });
}

@Test
void preventFileSystemAccess() {
    assertThrows(SecurityException.class, () -> {
        runtime.registerObject("file")
                .object(new java.io.File("/etc/passwd"))
                .build();
    });
}
```

## Open Questions

1. **Null handling**:
    - Should PEEL support null values from Java?
    - Option A: Throw error on null
    - Option B: Add Null/None primitive type
    - Option C: Convert to optional/maybe type
    - **Recommendation**: Option A for MVP (explicit error), Option B if needed

2. **Method overloading**:
    - How to handle overloaded methods?
    - Currently: Require explicit parameter types in whitelist
    - Alternative: Try to resolve based on argument types at runtime
    - **Recommendation**: Explicit types (safer, clearer)

3. **Static methods**:
    - Allow calling static methods?
    - Example: `Math.abs(-5)`
    - **Recommendation**: Yes, but same whitelist rules apply

4. **Field assignment**:
    - Syntax for writing fields?
    - Option A: `obj.field = value` (assignment statement)
    - Option B: `obj.setField(value)` (method call only)
    - **Recommendation**: Option A for consistency with variables

5. **Exception handling**:
    - How much Java exception detail to expose?
    - Stack traces visible to PEEL code?
    - **Recommendation**: Wrap in PeelException, log details, show message only

6. **Performance**:
    - MethodHandles vs Reflection?
    - Caching of type conversions?
    - **Recommendation**: Use MethodHandles (faster, safer)

7. **Chaining**:
    - Support method chaining? `obj.method1().method2()`
    - Requires intermediate objects also be ExternalObjects
    - **Recommendation**: Yes, natural for fluent APIs

8. **Array handling**:
    - Java arrays vs List?
    - **Recommendation**: Convert arrays to PeelValue.Collection.List

9. **Varargs**:
    - Support Java varargs methods?
    - **Recommendation**: Not for initial implementation

10. **Documentation**:
    - How do users discover what's available on registered objects?
    - Runtime introspection API?
    - **Recommendation**: Good error messages, no introspection (security)

## Definition of Done

- [ ] SecurityValidator implemented and tested
- [ ] ObjectRegistry and registration API implemented
- [ ] ExternalObject PeelValue type implemented
- [ ] Grammar supports field access and method calls
- [ ] Parser creates correct AST nodes
- [ ] Runtime evaluates field access correctly
- [ ] Runtime evaluates method calls correctly
- [ ] Type conversion Java ↔ PEEL works
- [ ] Whitelist enforcement works (positive tests)
- [ ] Security tests pass (negative tests)
- [ ] All dangerous types blocked
- [ ] Reflection completely blocked
- [ ] Good error messages for security violations
- [ ] Good error messages for type mismatches
- [ ] Documentation includes security guidelines
- [ ] Security review completed
- [ ] Integration tests with real use cases

## Security Review Checklist

Before releasing this feature:

- [ ] Security expert review of SecurityValidator
- [ ] Penetration testing by someone trying to break it
- [ ] Code review focused on security
- [ ] Documentation warns about dangers
- [ ] Examples show safe patterns only
- [ ] Unsafe patterns explicitly documented as forbidden
- [ ] Consider bug bounty for finding exploits

## Migration / Integration Notes

This epic requires users to:

1. Update registration code to use new API
2. Explicitly whitelist methods/fields
3. Handle potential security exceptions

**Breaking change**: If current `SimpleRuntime.register(Variable)` allows arbitrary objects, this tightens security
significantly.

**Recommendation**: Provide migration helper that logs what needs to be whitelisted.

## Future Enhancements (Not in this Epic)

- Custom type converters for complex types
- Annotation-based whitelisting on Java side
- Sandboxed execution environment
- Resource limits (CPU, memory, time)
- Audit logging of object access
- Permission system (different PEEL scripts get different access)
