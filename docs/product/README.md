# PEEL MVP - Epic Overview

This directory contains the epic planning for PEEL's MVP (0.1 Release).

## Epic Structure

The MVP is organized into 6 epics, executed sequentially. Each epic builds on the previous ones, maintaining a
functioning language at every step.

### Epic 1: Branching

**File**: `EPIC-01-branching.md`
**Dependencies**: None
**Goal**: Add if/else conditional statements and ternary operator

Introduces control flow to enable decision-making in programs. Keeps scoping simple (flat HashMap).

**Key Features**:

- If statement: `if (condition) { }`
- If-else statement: `if (condition) { } else { }`
- Ternary operator: `condition ? trueExpr : falseExpr`

**Complexity**: Low - straightforward addition to grammar and runtime

---

### Epic 2: Loops

**File**: `EPIC-02-loops.md`
**Dependencies**: Epic 1 (needs boolean expressions)
**Goal**: Add while and for loops for iteration

Completes basic control flow primitives. Includes break/continue for loop control.

**Key Features**:

- While loop: `while (condition) { }`
- For loop: `for (init; condition; increment) { }`
- Break and continue statements

**Complexity**: Medium - requires handling non-local control flow (break/continue)

**Open Design Question**: How to represent loop iterations in `EvaluatedStatement` for "printable" output?

---

### Epic 3: Lexical Scoping

**File**: `EPIC-03-lexical-scoping.md`
**Dependencies**: Epic 1, 2 (refactors existing code)
**Goal**: Implement proper lexical scoping with scope chain

**Major refactoring** of `SimpleRuntime`. Replaces flat `HashMap<String, EvaluatedExpression>` with scope chain
supporting:

- Block-level scoping
- Variable shadowing
- Proper variable lifetime
- Foundation for closures (Epic 4)

**Key Features**:

- Block scopes: `{ }` creates new scope
- Variable shadowing
- If/else/loop scopes
- For-loop initialization scope

**Complexity**: High - significant runtime architecture change, must ensure all existing tests pass

**Open Design Questions**:

- Assignment vs declaration semantics
- For-loop variable scope
- Explicit `let` keyword or implicit scoping?

---

### Epic 4: First-Class Functions

**File**: `EPIC-04-first-class-functions.md`
**Dependencies**: Epic 3 (absolutely requires scope chain for closures)
**Goal**: Add user-defined functions as first-class values with closures

Completes the "complete programming language" goal. Most complex epic due to closures and recursion.

**Key Features**:

- Function definition: `fn(params) { body }`
- Function calls (user-defined and built-in)
- Closures (capture outer scope)
- Return statement: `return expr;`
- Recursion support

**Complexity**: Very High - involves new value types, scope capture, parameter binding, non-local returns

**Open Design Questions**:

- Implicit vs explicit return
- Void/unit type for functions without return
- Closure capture semantics (reference vs copy)
- Recursion name binding

---

### Epic 5: Extensibility

**File**: `EPIC-05-extensibility.md`
**Dependencies**: Epic 1-4 (refactors complete language)
**Goal**: Redesign runtime to make arithmetic and functions pluggable

Addresses the README concern: "users should not be forced to implement their own evaluation, scoping logic etc if they
don't like the arithmetic etc."

**Key Features**:

- Separate `Executor` (evaluation) from `ArithmeticEngine` (arithmetic)
- Pluggable arithmetic (custom number types, units, modular arithmetic)
- Enhanced function registry with type-based overloading
- Builder API for runtime configuration

**Architecture**:

```
ConfigurableRuntime
  ├─ Executor (evaluation, scoping) - provided by PEEL
  ├─ ArithmeticEngine - pluggable
  └─ FunctionRegistry - pluggable
```

**Complexity**: High - major refactoring but well-defined interfaces

**Open Design Questions**:

- **Operator registry**: Should users register custom operators? (Kept as open question)
- Primitive type extensibility (sealed interface limitation)
- Performance impact of pluggability

---

### Epic 6: Output Formats

**File**: `EPIC-06-output-formats.md`
**Dependencies**: Epic 1-5 (leverages complete language)
**Goal**: Generate JSON and other formats from `EvaluatedProgram`

Implements the "printable" aspect of PEEL by serializing evaluation trees.

**Key Features**:

- JSON formatter (MVP requirement)
- Text formatter (human-readable)
- Pluggable `OutputFormatter` interface
- Verbosity configuration (minimal/standard/verbose)
- Loop iteration limiting

**Complexity**: Medium - straightforward serialization, main challenge is design decisions

**Open Design Questions**:

- JSON schema definition and versioning
- Loop iteration detail (all vs summary)
- Error representation in output
- Source location tracking

---

## Development Principles

1. **Test-Driven**: Each epic specifies tests first, implementation follows
2. **Incremental**: Language functions at every step, no broken states
3. **One Epic at a Time**: Complete and stabilize before moving to next
4. **Defer Complexity**: Architectural issues (runtime, scoping) tackled when needed, not prematurely

## MVP Completion Criteria

After Epic 6:

- ✅ Complete programming language (branching, loops, variables, functions)
- ✅ Lexical scoping with closures
- ✅ First-class functions
- ✅ Easy function registration from host
- ✅ Extensible runtime (pluggable arithmetic)
- ✅ JSON output format
- ✅ Good test coverage

## Estimated Epic Sizes

**Small**: Epic 1
**Medium**: Epic 2, Epic 6
**Large**: Epic 3, Epic 5
**Very Large**: Epic 4

**Total MVP**: ~6 epics, no deadline (hobby project pace)

---

## Notes

- Each epic file contains detailed technical implementation, test plans, and open questions
- Open questions should be resolved during epic execution, not before
- Some decisions intentionally deferred (e.g., operator registry in Epic 5)
- Architecture evolves: Simple → Scoped (Epic 3) → Pluggable (Epic 5)
