# PEEL 🍌

**P**rintable, **E**xtendable **E**xpression **L**anguage

PEEL is an expression language with the goal to provide a language, that is easy and safe to integrate into applications
while the
calculations are easily printable.

*Printable*: The result of the calculation is not a single value, but the whole execution trace. This can be used to
*Extendable*: The language / runtime can easily be adjusted for the use case. Rounding-Modes can be set and further
functions easily provided.

### Example Use Case

An example use case would be an expert-system, where domain experts can implement snippets of business logic themselves,
while the system provides access to data or "pre-built" logic implemented in another language.
The output of the calculation is a structured "calculation-sheet", that experts can use to verify or can be printed
to documents for customers or regulators.


### Language Features

* Dynamic Typing
* First-Class Functions and Closures
* Access to objects provided by the runtime

## Current Status

**MVP in Development** - Implementing core language features (Epics 1-6):

- ✅ Basic expressions and operators
- ✅ Variables and assignments
- ✅ Control flow (if/else, loops)
- ✅ Lexical scoping
- ✅ First-class functions
- ✅ Extensible runtime architecture
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

## Requirements

- **Java 22** - Uses modern pattern matching and sealed types
- **Gradle** - Build system
- **ANTLR 4** - Parser generator

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


