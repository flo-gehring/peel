# Epic 2: Control Flow - Loops

**Status**: Not Started
**Dependencies**: Epic 1 (Branching) - needs if statements and boolean expressions
**Goal**: Add loop constructs to enable iteration and repeated execution.

## Overview

Implement while and for loops to allow iterative computation. This builds on the conditional logic from Epic 1 and
completes the basic control flow primitives before introducing lexical scoping.

## Features

### 1. While Loop

- **Description**: Repeat a block while a condition is true
- **Syntax**: `while (condition) { statements }`
- **Grammar changes**: Add `whileStatement` rule
- **Test cases**:
    - Basic while loop executes multiple times
    - While loop with false condition never executes
    - While loop modifying condition variable
    - Nested while loops
    - While loop with break (if we add it)

### 2. For Loop (C-style)

- **Description**: Loop with initialization, condition, and increment
- **Syntax**: `for (init; condition; increment) { statements }`
- **Grammar changes**: Add `forStatement` rule
- **Test cases**:
    - Basic for loop counting
    - For loop with complex increment
    - Empty for loop sections
    - Nested for loops

### 3. Loop Control (Optional for this Epic)

- **Description**: Break and continue statements
- **Syntax**: `break;` and `continue;`
- **Decision needed**: Include in Epic 2 or defer?
- **Test cases**:
    - Break exits loop
    - Continue skips to next iteration
    - Break/continue in nested loops

## Technical Implementation

### Grammar Changes (Peel.g4)

```antlr
statement
    : assignment
    | ifStatement
    | whileStatement
    | forStatement
    | breakStatement
    | continueStatement
    | expr ';'
    ;

whileStatement
    : 'while' '(' expr ')' block
    ;

forStatement
    : 'for' '(' (assignment | expr ';')? expr? ';' expr? ')' block
    ;

breakStatement
    : 'break' ';'
    ;

continueStatement
    : 'continue' ';'
    ;
```

### AST Changes

Add to `Statement` sealed interface:

```java
record WhileStatement(Expression condition, List<CodeElement> body)
        implements Statement {
}

record ForStatement(
        Statement init,           // can be assignment or expression statement
        Expression condition,     // nullable
        Expression increment,     // nullable
        List<CodeElement> body
) implements Statement {
}

record BreakStatement() implements Statement {
}

record ContinueStatement() implements Statement {
}
```

### Runtime Changes

**Challenge**: Break/continue need non-local control flow.

Options:

1. **Exceptions**: Throw `BreakException`/`ContinueException` and catch in loop
2. **Return value**: Return special sentinel from evaluation
3. **Control flow flag**: Pass mutable state through evaluation

Extend `SimpleRuntime.runStatement()`:

- `WhileStatement`: Loop while condition is true
- `ForStatement`: Execute init, then loop with condition and increment
- Handle break/continue if implemented

### Visitor Changes

Extend `ProgrammVisitor` with:

- `visitWhileStatement()`
- `visitForStatement()`
- `visitBreakStatement()` (if included)
- `visitContinueStatement()` (if included)

### Evaluated Forms

TODO: Below is the first draft for Loops, however, this has a flaw

- the Evaluated Expression for the condition appears only once. which evaluation is it?
- The first? the last? need to rethink the structure.

Add to `EvaluatedStatement`:

```java
record WhileLoop(
        EvaluatedExpression condition,
        List<EvaluatedCodeElement> iterations  // each iteration's evaluated body
) implements EvaluatedStatement {
}

record ForLoop(
        EvaluatedStatement init,
        EvaluatedExpression condition,
        List<EvaluatedCodeElement> iterations
) implements EvaluatedStatement {
}
```

**Design question**: Do we capture every iteration in EvaluatedStatement for "printable" output?

- Could get large for many iterations
- Might want to limit or make configurable
-

## Test-Driven Development

### Test Order

1. **WhileLoopTest**: Basic while loop functionality
2. **WhileLoopConditionTest**: Condition evaluation and exit
3. **ForLoopTest**: Basic for loop counting
4. **NestedLoopTest**: Nested loops
5. **BreakContinueTest**: Loop control (if implemented)
6. **PeelGrammarTest**: Parser tests for loop syntax

### Example Test Cases

```java

@Test
void whileLoopCountsToFive() {
    Program program = PeelGrammar.parse("""
                x = 0;
                while (x < 5) {
                    x = x + 1;
                }
                x;
            """);
    SimpleRuntime runtime = RuntimeFactory.defaultLanguage();
    EvaluatedProgram result = runtime.run(program);
    // Assert x is 5
}

@Test
void forLoopSumNumbers() {
    Program program = PeelGrammar.parse("""
                sum = 0;
                for (i = 0; i < 10; i = i + 1) {
                    sum = sum + i;
                }
                sum;
            """);
    // Assert sum is 45
}

@Test
void nestedLoopMultiplication() {
    Program program = PeelGrammar.parse("""
                result = 0;
                for (i = 1; i < 4; i = i + 1) {
                    for (j = 1; j < 4; j = j + 1) {
                        result = result + (i * j);
                    }
                }
                result;
            """);
    // Assert result is 36 (sum of all i*j products)
}
```

TODO add Tests for

## Open Questions

1. **Break/Continue**: Include in this epic or defer?
    - **Recommendation**: Include - they're natural with loops and help testing
    - Implementation via exceptions is straightforward

2. **For loop variants**:
    - Just C-style for, or also for-each over collections?
    - **Recommendation**: Start with C-style, for-each needs collection iteration (Epic 4?)

3. **Infinite loop protection**: Should runtime have max iteration count?
    - **Recommendation**: Not for MVP, trust the programmer

4. **EvaluatedStatement size**: Capture all iterations or summary?
    - **Recommendation**: Start with capturing all, optimize later if needed

5. **Do-while loop**: Include or just while?
    - **Recommendation**: Just while for simplicity, do-while is rare

## Definition of Done

- [ ] Grammar supports while and for loops
- [ ] Parser creates correct AST nodes
- [ ] Runtime executes loops correctly
- [ ] Break and continue work (if implemented)
- [ ] All test cases pass
- [ ] Nested loops work
- [ ] EvaluatedStatement captures loop execution (decide on iteration detail)
- [ ] Loop condition must be Bool (type checking)
- [ ] Documentation updated

## Notes for Next Epic

After loops are working, Epic 3 (Lexical Scoping) will need to address:

- For loop initialization should create loop-scoped variable
- Block-level scoping within loop bodies
- This epic can ignore scoping by using flat HashMap (variables leak)
