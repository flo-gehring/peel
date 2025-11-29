# Epic 6: Output Formats (JSON and Beyond)

**Status**: Not Started
**Dependencies**: Epic 1-5 (Complete language with clean architecture)
**Goal**: Generate structured output formats (starting with JSON) from `EvaluatedProgram` to enable the "printable"
aspect of PEEL.

## Overview

The "printable" in PEEL means accessing intermediate results and calculations. The `EvaluatedExpression` and
`EvaluatedStatement` trees already capture this information - they record every step of evaluation.

This epic is about **serializing** these evaluation trees into various output formats:

- JSON (MVP requirement)
- HTML (for web display)
- Markdown (for documentation)
- Plain text (human-readable debug output)

## Features

### 1. JSON Output Format

- **Description**: Serialize `EvaluatedProgram` to JSON
- **Requirements**:
    - All primitive values represented
    - All collections represented
    - Evaluation tree structure preserved
    - Intermediate results visible
    - Each statement/expression has:
        - Type information
        - Final value
        - Sub-expressions/statements (nested)
- **Example**:
  ```json
  {
    "type": "Program",
    "statements": [
      {
        "type": "Assignment",
        "variable": "x",
        "value": {
          "type": "BinaryOperator",
          "operator": "+",
          "lhs": {"type": "Literal", "value": 1},
          "rhs": {"type": "Literal", "value": 2},
          "result": 3
        }
      },
      {
        "type": "Expression",
        "value": {
          "type": "Variable",
          "name": "x",
          "result": 3
        }
      }
    ]
  }
  ```

### 2. Output Formatter Interface

- **Description**: Pluggable output formatters
- **API**:
  ```java
  interface OutputFormatter {
      String format(EvaluatedProgram program);
      String formatExpression(EvaluatedExpression expr);
      String formatStatement(EvaluatedStatement stmt);
  }
  ```

### 3. Multiple Format Support

- **JSON Formatter**: Structured data
- **Text Formatter**: Human-readable output
- **HTML Formatter**: Web display with syntax highlighting
- **Markdown Formatter**: Documentation-friendly

### 4. Configuration Options

- **Verbosity levels**:
    - Minimal: Only final results
    - Standard: Show intermediate calculations
    - Verbose: Show every step including type info
- **Filtering**:
    - Include/exclude certain statement types
    - Limit depth of nested expressions
    - Truncate large loops (don't show all iterations)

## Technical Implementation

### OutputFormatter Interface

```java
interface OutputFormatter {
    String format(EvaluatedProgram program);
    String format(EvaluatedExpression expr);
    String format(EvaluatedStatement stmt);
    String format(PeelValue value);
}

class FormatterConfig {
    enum Verbosity { MINIMAL, STANDARD, VERBOSE }

    private Verbosity verbosity = Verbosity.STANDARD;
    private int maxDepth = Integer.MAX_VALUE;
    private int maxIterations = 100;  // for loops

    // ... getters/setters
}
```

### JSON Formatter Implementation

```java
class JsonFormatter implements OutputFormatter {
    private final FormatterConfig config;
    private final ObjectMapper mapper;  // or manual JSON building

    @Override
    public String format(EvaluatedProgram program) {
        JsonObject root = new JsonObject();
        root.put("type", "Program");
        JsonArray statements = new JsonArray();
        for (EvaluatedCodeElement element : program.elements()) {
            statements.add(formatElement(element));
        }
        root.put("statements", statements);
        return root.toString();
    }

    private JsonObject formatElement(EvaluatedCodeElement element) {
        return switch(element) {
            case EvaluatedExpression expr -> formatExpression(expr);
            case EvaluatedStatement stmt -> formatStatement(stmt);
        };
    }

    // ... implementations for each evaluated type
}
```

### Text Formatter Implementation

```java
class TextFormatter implements OutputFormatter {
    @Override
    public String format(EvaluatedProgram program) {
        StringBuilder sb = new StringBuilder();
        for (EvaluatedCodeElement element : program.elements()) {
            sb.append(formatElement(element));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatExpression(EvaluatedExpression expr) {
        return switch(expr) {
            case EvaluatedExpression.Literal(var value) ->
                formatValue(value);
            case EvaluatedExpression.BinaryOperator(var op, var lhs, var rhs, var result) ->
                String.format("%s %s %s = %s",
                    formatExpression(lhs),
                    op,
                    formatExpression(rhs),
                    formatValue(result));
            // ... other cases
        };
    }
}
```

### EvaluatedExpression Changes

Current `EvaluatedExpression` might need enhancement to include results:

```java
sealed interface EvaluatedExpression {
    PeelValue result();  // Add result to all variants

    record Literal(PeelValue value) implements EvaluatedExpression {
        @Override
        public PeelValue result() { return value; }
    }

    record BinaryOperator(
        String operator,
        EvaluatedExpression lhs,
        EvaluatedExpression rhs,
        PeelValue result  // Add result field
    ) implements EvaluatedExpression {}

    // ... similar for other expression types
}
```

### Runtime API Enhancement

Add output formatting to Runtime API:

```java
interface Runtime {
    EvaluatedProgram run(Program program);

    // New methods
    String runAndFormat(Program program, OutputFormatter formatter);

    default String runAsJson(Program program) {
        return runAndFormat(program, new JsonFormatter());
    }

    default String runAsText(Program program) {
        return runAndFormat(program, new TextFormatter());
    }
}
```

## Test-Driven Development

### Test Order

1. **JsonFormatterTest**: Test JSON output for each AST node type
2. **TextFormatterTest**: Test text output
3. **FormatterConfigTest**: Test verbosity and filtering options
4. **IntegrationTest**: Full programs with various formatters
5. **EdgeCaseTest**: Large programs, deep nesting, long loops

### Example Test Cases

```java
@Test
void jsonOutputForSimpleExpression() {
    Program program = PeelGrammar.parse("1 + 2;");
    SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
    String json = runtime.runAsJson(program);

    // Parse JSON and assert structure
    JsonObject result = parseJson(json);
    assertEquals("Program", result.getString("type"));
    // ... more assertions
}

@Test
void jsonOutputShowsIntermediateCalculations() {
    Program program = PeelGrammar.parse("""
        a = 1 + 2;
        b = 3 + 4;
        a + b;
    """);
    String json = runtime.runAsJson(program);

    // Assert JSON shows:
    // - a = 3 (with 1 + 2 visible)
    // - b = 7 (with 3 + 4 visible)
    // - result = 10 (with a + b visible)
}

@Test
void textFormatterReadableOutput() {
    Program program = PeelGrammar.parse("a = 1 + 2; a * 3;");
    String text = runtime.runAsText(program);

    assertThat(text).contains("1 + 2 = 3");
    assertThat(text).contains("a = 3");
    assertThat(text).contains("3 * 3 = 9");
}

@Test
void minimalVerbosityOnlyShowsFinalResults() {
    FormatterConfig config = new FormatterConfig()
        .setVerbosity(Verbosity.MINIMAL);
    JsonFormatter formatter = new JsonFormatter(config);

    Program program = PeelGrammar.parse("1 + 2 + 3;");
    String json = runtime.runAndFormat(program, formatter);

    // Should show result: 6, not intermediate 1 + 2 = 3
}
```

## Open Questions

1. **JSON schema**:
    - Define formal JSON schema for output?
    - Version the schema?
    - **Recommendation**: Yes, document schema, version for future changes

2. **Streaming output**:
    - For long programs, stream output instead of building entire JSON?
    - **Recommendation**: Not for MVP, build in memory

3. **Loop iteration detail**:
    - Show all iterations or summary?
    - Configurable limit on iterations shown?
    - **Recommendation**: Configurable, default limit (e.g., 100 iterations max)

4. **Error representation**:
    - How to represent errors in JSON output?
    - Include partial evaluation before error?
    - **Recommendation**: Yes, include what was evaluated before error

5. **Source location**:
    - Include line/column numbers from source?
    - **Recommendation**: Nice to have but not critical for MVP

6. **File output**:
    - Support writing to files directly?
    - **Recommendation**: User can do this, just provide string

7. **Pretty printing**:
    - Always pretty-print JSON or make configurable?
    - **Recommendation**: Configurable, default to pretty for readability

## Definition of Done

- [ ] OutputFormatter interface defined
- [ ] JsonFormatter implemented for all AST node types
- [ ] TextFormatter implemented
- [ ] FormatterConfig with verbosity levels
- [ ] Loop iteration limiting works
- [ ] Runtime.runAsJson() and runAsText() methods
- [ ] All test cases pass
- [ ] JSON output is valid JSON
- [ ] Documentation includes JSON schema
- [ ] Examples of all output formats
- [ ] Performance acceptable for reasonable program sizes

## MVP Completion

After this epic, PEEL MVP is complete:

- ✅ Complete programming language (branching, loops, variables, functions)
- ✅ Lexical scoping
- ✅ First-class functions
- ✅ Easy way to register functions from host
- ✅ Extensible runtime architecture
- ✅ JSON output format
- ✅ Good test coverage (test-driven throughout)

## Future Work (Post-MVP)

Nice-to-haves that aren't MVP:

- HTML formatter with syntax highlighting
- Markdown formatter
- Interactive output (web UI)
- Graphical representation of evaluation tree
- Export to other formats (CSV for data, etc.)
- Real-time streaming for long-running programs
- Debugging support (step through evaluation)
