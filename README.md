~~# PEEL 🍌

**P**rintable, **E**xtendable **E**xpression **L**anguage

PEEL is an expression language designed for expert systems in regulated industries. It enables domain experts to write
calculation logic that is both executable and auditable, with built-in traceability of intermediate results.

## Vision

In banking, insurance, and risk management, experts need to implement complex calculations and business rules.
Currently, they use Excel spreadsheets - which work for exploration but fail at governance, auditability, and
integration.

PEEL bridges this gap by providing:

- **Printable**: Every calculation step is captured and can be rendered into compliance documents
- **Extendable**: Plug in custom arithmetic, functions, and domain-specific logic
- **Auditable**: Scripts stored in databases with version control and approval workflows
- **Integrated**: Embed calculations into existing systems via Java API~~

Think of it as "better integrated Excel for production systems."

## Key Features

### Printable Execution

Every evaluation produces a tree of intermediate results, not just a final value:

```java
Program program = PeelGrammar.parse("""
            principal = 100000;
            rate = 0.05;
            years = 30;
            monthlyPayment = principal * (rate / 12) * (1 + rate/12)^(years*12) / ((1 + rate/12)^(years*12) - 1);
            monthlyPayment;
        """);

EvaluatedProgram result = runtime.run(program);
String jsonOutput = result.toJSON();  // Shows ALL intermediate calculations
```

The JSON output includes every step: `principal = 100000`, `rate = 0.05`, the calculation of `monthlyPayment`, etc.
Perfect for generating compliance reports or explaining results to customers.

### Extendable Runtime

Customize arithmetic without reimplementing evaluation:

```java
Runtime runtime = RuntimeBuilder.create()
        .withArithmetic(new DecimalArithmetic())  // Custom number handling
        .registerFunction("npv", netPresentValueFunc)  // Domain-specific functions
        .registerFunction("irr", internalRateOfReturnFunc)
        .build();
```

Experts write scripts, IT provides the building blocks.

### Type-Safe & Fast

- Written in Java 22 with modern language features
- ANTLR-based parser for robust syntax
- Sealed types for exhaustive pattern matching
- First-class functions with closures
- Lexical scoping

## Quick Example

```javascript
// PEEL script for loan approval
creditScore = applicant.getCreditScore();
debtToIncome = applicant.getDebt() / applicant.getIncome();

if (creditScore > 700 && debtToIncome < 0.43) {
    approved = true;
    rate = baseRate - 0.5;
} else if (creditScore > 650 && debtToIncome < 0.35) {
    approved = true;
    rate = baseRate;
} else {
    approved = false;
    rate = 0;
}
```

This script can be:

- Stored in a database
- Evaluated with different applicant data
- Rendered into a decision document showing every calculation step
- Versioned and audited for compliance

## Current Status

**MVP in Development** - Implementing core language features (Epics 1-6):

- ✅ Basic expressions and operators
- ✅ Variables and assignments
- 🚧 Control flow (if/else, loops)
- 🚧 Lexical scoping
- 🚧 First-class functions
- 🚧 Extensible runtime architecture
- 🚧 JSON output format

See [`docs/product/`](docs/product/) for detailed epic planning.

## Getting Started

### Build & Run

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate ANTLR parser (automatically done by build)
./gradlew generateGrammarSource
```

### Basic Usage

```java
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.run.SimpleRuntime;
import de.flogehring.peel.convenience.RuntimeFactory;

// Parse PEEL source code
Program program = PeelGrammar.parse("""
            x = 10;
            y = 20;
            x + y;
        """);

        // Create runtime with standard functions
        SimpleRuntime runtime = RuntimeFactory.defaultLanguage();

        // Evaluate
        EvaluatedProgram result = runtime.run(program);
System.out.

        println(result);  // Shows: x = 10, y = 20, result = 30
```

## Documentation

- **[CLAUDE.md](CLAUDE.md)** - Architecture overview and development guide
- **[docs/product/](docs/product/)** - MVP epic planning (Epics 1-6)
- **[docs/product/backlog/](docs/product/backlog/)** - Post-MVP features and roadmap
- **[src/main/antlr/Peel.g4](src/main/antlr/Peel.g4)** - Grammar specification

## Requirements

- **Java 22** - Uses modern pattern matching and sealed types
- **Gradle** - Build system
- **ANTLR 4** - Parser generator

## Architecture Highlights

PEEL separates concerns cleanly:

1. **Language Representation** (`core.lang`) - Immutable AST nodes
2. **Value System** (`core.values`) - Sealed value types (Number, Text, Bool, Collections)
3. **Evaluation** (`core.eval`) - Runtime interface for execution
4. **Parsing** (`parse`) - ANTLR-based parser with visitor pattern
5. **Convenience Layer** (`convenience`) - Pre-configured runtimes and helpers

The architecture supports pluggable arithmetic engines and function registries, enabling domain-specific customization
without forking the core evaluator.

## Use Cases

### Expert Systems

Domain experts write calculation logic in PEEL, stored in databases, evaluated as part of workflows.

### Compliance & Auditability

Every calculation step is captured, enabling automatic generation of compliance documents showing "how we got this
number."

### Business Rules

Complex decision logic that needs to be versioned, tested, and audited independently of the main application code.

### Template Calculations

Define a calculation once, evaluate it thousands of times with different input data (e.g., pricing models, risk
calculations).

## Roadmap

**Version 0.1 (MVP)** - Core language features

- Complete programming language (branching, loops, functions)
- Lexical scoping with closures
- Extensible runtime
- JSON output format

**Version 0.2** - Performance & Usability

- Compiled programs (parse once, evaluate many times)
- Standard library (math, string, collection functions)
- REPL for interactive development

**Version 0.3** - Integration

- REST API for evaluation
- HTML output formatter
- Partial evaluation on errors (debugging)

See [docs/product/backlog/](docs/product/backlog/) for full roadmap.

## Contributing

PEEL is currently in active development toward the 0.1 MVP. The architecture is stabilizing, but significant changes are
expected during Epic 3 (Lexical Scoping) and Epic 5 (Extensibility).

For architecture questions or feature discussions, see [CLAUDE.md](CLAUDE.md) or open an issue.

## License

[License information to be added]

---

**PEEL**: Making expert calculations auditable, maintainable, and production-ready.
