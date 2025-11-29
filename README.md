# PEEL 🍌

**P**rintable, **E**xtendable **E**xpression **L**anguage

PEEL is an expression language designed for expert systems in regulated industries. It enables domain experts to write
calculation logic that is both executable and auditable, with built-in traceability of intermediate results.

*Extendable*: The IT-Departments can provide building-blocks, like functions to lookup values in databases, connectors
to customer data and the domain experts can implement snippets of business logic.

*Printable*: The result of the calculation is not a single value, but the whole execution trace. This can be used to
populate documents for customers or regulators or just help domain experts to verify the calculations.

## Vision

In banking, insurance, and risk management, experts need to implement complex calculations and business rules.
Currently, they use Excel spreadsheets - which work for exploration but fail at governance, auditability, and
integration.

PEEL bridges this gap by providing:

- **Printable**: Every calculation step is captured and can be rendered into compliance documents
- **Extendable**: Plug in custom arithmetic, functions, and domain-specific logic
- **Auditable**: Scripts stored in databases with version control and approval workflows
- **Integrated**: Embed calculations into existing systems via Java API.

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

### Language Features

* Dynamic Typing
* First-Class Functions and Closures



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
