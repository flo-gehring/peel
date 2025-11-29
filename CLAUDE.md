# CLAUDE.md

## Instructions for Claude

* You're a critical and honest software engineer.
* You don't hold back when you think something is not conceptually sound or implemented less than perfect.
* You try to help your colleagues with good advice.
* Your comments are short and to the point. No unnecessary fluff.
* You know that communication is hard. If you don't understand something 100% then you ask and don't assume.
* You value feedback loops. Your implementation approach is therefore test driven.

## Project Overview

PEEL (Printable, Extendable Expression Language) is a hobby project building a configurable expression language that
allows users to access and display intermediate results and calculations. The project is written in Java 22 and uses
ANTLR for parsing.

## Build and Development Commands

### Basic Commands

- **Build the project**: `./gradlew build`
- **Run tests**: `./gradlew test`
- **Generate ANTLR parser**: `./gradlew generateGrammarSource`
    - Output directory: `build/generated-src/antlr/main/`
    - Grammar file: `src/main/antlr/Peel.g4`
    - Generates visitor pattern classes

### Running Single Tests

Use JUnit 5 test filtering:

```bash
./gradlew test --tests "de.flogehring.peel.parse.PeelGrammarTest.simple"
```

### Clean Build

```bash
./gradlew clean build
```

## Architecture

### Core Components

The codebase follows a clear separation between language representation, evaluation, and parsing:

**1. Language Representation (core.lang)**

- `Program`: Container for a list of `CodeElement` instances
- `CodeElement`: Sealed interface representing either `Expression` or `Statement`
- `Expression`: Sealed interface with variants:
    - `Literal`: Wraps a `PeelValue`
    - `BinaryOperator`: Represents operations like `+`, `-`, `*`, `/`, `&&`, `||`, etc.
    - `VariableName`: Variable reference
    - `FunctionCall`: Function invocation with arguments
- `Statement`: Sealed interface with variants:
    - `Assignment`: Variable assignment (`x = expr`)

**2. Value System (core.values)**

- `PeelValue`: Sealed interface representing all PEEL values
    - `Primitives`: Sealed interface for primitive types (`Number`, `Text`, `Bool`)
        - `Number`: Sealed interface with `Integer`, `Float`, and `Decimal` variants
        - `Text`: String values
        - `Bool`: Boolean values
    - `Collection`: Sealed interface for collections
        - `List`: Ordered collection of `PeelValue`
        - `Map`: Key-value pairs with `Primitives` as keys

**3. Evaluation (core.eval)**

- `Runtime`: Interface for executing programs
    - `register(Variable)`: Register global variables
    - `register(Function)`: Register functions (supports overloading by arity)
    - `run(Program)`: Execute a program and return `EvaluatedProgram`
- `SimpleRuntime`: Main implementation that:
    - Maintains `HashMap<String, EvaluatedExpression>` for variables
    - Maintains `HashMap<String, List<Function>>` for functions (overloading support)
    - Evaluates expressions recursively
    - Treats binary operators as function calls

**4. Parsing (parse)**

- `PeelGrammar`: Uses ANTLR-generated lexer/parser
    - `parse(String)`: Parses PEEL source code into a `Program`
    - `ProgrammVisitor`: Implements ANTLR visitor pattern to build AST
- Grammar supports:
    - Arithmetic: `+`, `-`, `*`, `/`
    - Logic: `&&`, `||`, `!`, `^`, `==`
    - Variables: `x = expr;`
    - Expressions: `expr;`
    - Comments: `// ...`

**5. Convenience Layer (convenience)**

- `RuntimeFactory.defaultLanguage()`: Creates pre-configured runtime with:
    - Addition operator for numbers, text, lists, and maps
    - `count` function for substring counting
- `FunctionFactory`: Helpers for creating function implementations

### Key Design Patterns

**Sealed Interfaces**: Heavily used for exhaustive pattern matching in switch expressions

- All `CodeElement`, `Expression`, `Statement`, `PeelValue`, and `Number` types are sealed

**Visitor Pattern**: ANTLR visitor pattern in `ProgrammVisitor` converts parse tree to domain objects

**Function Overloading**: Functions are stored by name with multiple implementations differing by arity

- Runtime throws `NoFunctionFoundException` if no matching function found
- Runtime throws `MultipleFunctionsFoundException` if ambiguous match

**Operators as Functions**: Binary operators like `+` are registered as functions in the runtime

- This allows extensibility - new operators can be registered as functions

### Testing

Tests use JUnit 5 with AssertJ for assertions:

- `PeelGrammarTest`: Parser tests
- `SimpleRuntimeTest`: Runtime execution tests

## MVP Roadmap

See `docs/product/EPIC-*.md` for detailed planning:

1. **Epic 1 - Branching**: If/else statements, ternary operator
2. **Epic 2 - Loops**: While and for loops, break/continue
3. **Epic 3 - Lexical Scoping**: Major runtime refactor for proper scoping
4. **Epic 4 - First-Class Functions**: User-defined functions with closures
5. **Epic 5 - Extensibility**: Pluggable arithmetic and function registry
6. **Epic 6 - Output Formats**: JSON serialization of evaluation tree

**Development Approach**: Test-driven, one epic at a time. Language must function at every step.

## The "Printable" Concept

PEEL's "printable" feature is about capturing intermediate results:

- Every evaluation creates `EvaluatedExpression` and `EvaluatedStatement` trees
- These trees preserve the evaluation structure (not just final values)
- Example: `a = 1 + 2` captures both the `1 + 2` operation and its result `3`
- Epic 6 will serialize these trees to JSON and other formats for display

## Known Architectural Issues

**Runtime Redesign Needed (Epic 5)**:

- Current `SimpleRuntime` mixes evaluation logic with arithmetic/function handling
- Makes customization (e.g., different arithmetic) require reimplementing everything
- Plan: Separate `Executor` (evaluation) from `ArithmeticEngine` and `FunctionRegistry`
- This is intentionally deferred until language features are complete

**Scoping Limitations (Epic 3)**:

- Current: Flat `HashMap<String, EvaluatedExpression>` for variables
- All variables are global, no block scoping
- Epic 3 will introduce scope chain for proper lexical scoping
- Required before first-class functions (closures need scope capture)

## Important Notes

- Java 22 required (uses modern pattern matching and sealed types)
- ANTLR grammar changes require running `generateGrammarSource` before compilation
- The compile task automatically depends on ANTLR generation
- Grammar generates visitor classes in package `de.flogehring.peel.antlr`
- Operators have precedence defined in the grammar (see Peel.g4)
- Sealed interfaces mean exhaustive pattern matching in switch expressions
- `EvaluatedExpression` needs to capture results for "printable" output
