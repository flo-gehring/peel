# Epic 1: Control Flow - Branching

**Status**: Not Started
**Dependencies**: None (builds on current expression/statement foundation)
**Goal**: Add if/else conditional statements to create a functioning language with basic control flow.

## Overview

Implement conditional branching (if/else) to allow programs to make decisions. This is the first step toward a complete
programming language and keeps the implementation simple by reusing the existing flat variable scope.

## Features

### 1. If Statement

- **Description**: Execute a block of code when a condition is true
- **Syntax**: `if (condition) { statements }`
- **Grammar changes**: Add `ifStatement` rule
- **Test cases**:
    - Simple if with true condition executes body
    - Simple if with false condition skips body
    - If with complex boolean expressions (&&, ||, !)

### 2. If-Else Statement

- **Description**: Execute one of two blocks based on a condition
- **Syntax**: `if (condition) { statements } else { statements }`
- **Grammar changes**: Extend `ifStatement` with optional else clause
- **Test cases**:
    - If-else with true condition executes if body
    - If-else with false condition executes else body
    - Nested if-else statements

### 3. Conditional Expression (Ternary)

- **Description**: Expression form of conditional for convenience
- **Syntax**: `condition ? exprTrue : exprFalse`
- **Grammar changes**: Add to expression rules
- **Test cases**:
    - Ternary returns correct value based on condition
    - Nested ternary expressions
    - Ternary in assignments

## Technical Implementation

### Grammar Changes (Peel.g4)

```antlr
statement
    : assignment
    | ifStatement
    | expr ';'
    ;

ifStatement
    : 'if' '(' expr ')' block ('else' block)?
    ;

block
    : '{' statement* '}'
    | statement
    ;

expr
    : expr '?' expr ':' expr    # ternaryExpr
    | expr '||' expr            # logicalOrExpr
    // ... rest of expressions
    ;
```

### AST Changes

Add to `Statement` sealed interface:

```java
record IfStatement(Expression condition, List<CodeElement> thenBlock, List<CodeElement> elseBlock)
    implements Statement {}
```

Add to `Expression` sealed interface (for ternary):

```java
record TernaryOperator(Expression condition, Expression thenExpr, Expression elseExpr)
    implements Expression {}
```

### Runtime Changes

Extend `SimpleRuntime.runStatement()` to handle if statements:

- Evaluate condition expression
- Cast to `Bool` (throw exception if not boolean)
- Execute appropriate block
- Return `EvaluatedStatement.IfStatement` with evaluated condition and executed branch

### Visitor Changes

Extend `ProgrammVisitor` with:

- `visitIfStatement()`
- `visitBlock()`
- `visitTernaryExpr()`

## Test-Driven Development

### Test Order

1. **IfStatementTest**: Basic if statement execution
2. **IfElseStatementTest**: If-else branching
3. **NestedIfTest**: Nested conditionals
4. **TernaryExpressionTest**: Ternary operator
5. **PeelGrammarTest**: Parser tests for new syntax

### Example Test Cases

```java
@Test
void ifStatementWithTrueCondition() {
    Program program = PeelGrammar.parse("""
        x = 0;
        if (1 == 1) {
            x = 5;
        }
        x;
    """);
    SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
    EvaluatedProgram result = runtime.run(program);
    // Assert x is 5
}

@Test
void ifElseStatement() {
    Program program = PeelGrammar.parse("""
        x = if (1 == 2) {
            10;
        } else {
            20;
        }
        x;
    """);
    // Assert x is 20
}
```

## Open Questions

1. **Block scope**: Do variables inside if blocks leak out, or introduce scoping now?
    - **Decision needed**: For Epic 1, keep flat scope (variables leak). Epic 3 will fix this.

2. **If as expression**: Should `if` return a value like Rust/Kotlin, or only statement form?
    - **Decision needed**: Start with statement form, evaluate expression form if useful.

3. **Else-if**: Support `else if` syntax or require `else { if {} }`?
    - **Decision needed**: Can start with nested syntax, add sugar later.

## Definition of Done

- [ ] Grammar supports if/else/ternary syntax
- [ ] Parser creates correct AST nodes
- [ ] Runtime executes conditionals correctly
- [ ] All test cases pass
- [ ] Boolean type checking (condition must be Bool)
- [ ] EvaluatedStatement captures branch taken and condition value
- [ ] Documentation updated
