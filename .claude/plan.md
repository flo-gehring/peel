# Implementation Plan: Epic 1 - Control Flow (Branching)

## Design Decisions

Based on discussion:

1. **If as expression**: If statements return the value of the last expression in the executed block
2. **Scoping**: Flat scope - variables leak out (Epic 3 will fix properly)
3. **Else-if**: Support `else if` as syntax sugar
4. **Ternary precedence**: Lowest (below `||`)

## Feature Breakdown

### Feature 1: Basic If Statement

**Goal**: Execute a block when condition is true

**Tasks**:

1. Add `IfStatement` record to `Statement.java`
    - Fields: `Expression condition`, `List<CodeElement> thenBlock`, `List<CodeElement> elseBlock` (nullable)

2. Add `EvaluatedStatement.IfStatement` to `EvaluatedStatement.java`
    - Fields: `EvaluatedExpression condition`, `List<EvaluatedCodeElement> executedBlock`, `boolean tookThenBranch`
    - This captures which branch was taken for "printable" output

3. Update grammar `Peel.g4`:
   ```antlr
   statement
       : assignment
       | ifStatement
       | expr ';'
       ;

   ifStatement
       : 'if' '(' expr ')' block
       ;

   block
       : '{' statement* '}'
       | statement
       ;
   ```

4. Extend `ProgrammVisitor` with `visitIfStatement()` and `visitBlock()`
    - Convert ANTLR parse tree to AST nodes

5. Extend `SimpleRuntime.runStatement()` to handle `IfStatement`:
    - Evaluate condition expression
    - Check condition is `Bool` (throw `IllegalArgumentException` if not)
    - Execute `thenBlock` if true
    - Return `EvaluatedStatement.IfStatement` with evaluated condition and executed branch

6. Write tests in `SimpleRuntimeTest`:
    - If with true condition executes body
    - If with false condition skips body
    - If with complex boolean expressions (`&&`, `||`, `!`)
    - Variables assigned in if block are visible after

7. Write parser tests in `PeelGrammarTest`:
    - Parse simple if statement
    - Parse if with block vs single statement

---

### Feature 2: If-Else Statement

**Goal**: Execute one of two blocks based on condition

**Tasks**:

1. Update grammar to add optional `else` clause:
   ```antlr
   ifStatement
       : 'if' '(' expr ')' block ('else' 'if' '(' expr ')' block)* ('else' block)?
       ;
   ```
    - This supports both `else if` sugar and final `else`

2. Update `ProgrammVisitor.visitIfStatement()`:
    - Handle `else if` chain by creating nested `IfStatement` nodes
    - Handle final `else` block

3. Update `SimpleRuntime` if-statement evaluation:
    - Execute `elseBlock` when condition is false
    - Update `EvaluatedStatement.IfStatement` to indicate which branch taken

4. Write tests:
    - If-else with true condition executes then branch
    - If-else with false condition executes else branch
    - Nested if-else statements
    - Else-if chain (3+ branches)

---

### Feature 3: If as Expression

**Goal**: If statements return the last expression value from executed block

**Tasks**:

1. Update `SimpleRuntime.runStatement()` for `IfStatement`:
    - Track last evaluated element in executed block
    - Extract its `PeelValue` (if expression) or use `null`/unit value (if statement)
    - Store in `EvaluatedStatement.IfStatement`

2. Add `EvaluatedStatement.value()` method:
    - Returns `PeelValue` for statements that produce values
    - `IfStatement` returns value of last element in executed block
    - `Assignment` returns assigned value
    - This allows `x = if (cond) { 5; } else { 10; };`

3. Update `SimpleRuntime.runStatement()` switch to handle statement expressions:
    - When evaluating `expr;` statement, check if `expr` is actually a statement-as-expression
    - May need to reconsider `CodeElement` hierarchy (statements can be expressions)

4. Write tests:
    - Assign result of if-else to variable
    - Use if-expression in larger expression context
    - If block with multiple statements returns last value
    - If without else returns null/unit when condition false

---

### Feature 4: Ternary Operator

**Goal**: Concise conditional expressions

**Tasks**:

1. Add `TernaryOperator` record to `Expression.java`:
    - Fields: `Expression condition`, `Expression thenExpr`, `Expression elseExpr`

2. Add `EvaluatedExpression.TernaryOperator` to `EvaluatedExpression.java`:
    - Fields: `EvaluatedExpression condition`, `EvaluatedExpression executedBranch`, `PeelValue value`,
      `boolean tookThenBranch`

3. Update grammar (ternary has lowest precedence):
   ```antlr
   expr
       : <assoc=right> expr '?' expr ':' expr  # ternaryExpr
       | expr '||' expr                        # logicalOrExpr
       | expr '&&' expr                        # logicalAndExpr
       // ... rest
       ;
   ```

4. Add `visitTernaryExpr()` to `ProgrammVisitor`

5. Extend `SimpleRuntime.runExpression()` to handle `TernaryOperator`:
    - Evaluate condition
    - Check condition is `Bool`
    - Evaluate and return appropriate branch
    - Return `EvaluatedExpression.TernaryOperator` with evaluated condition and chosen branch

6. Write tests:
    - Basic ternary returns correct value
    - Nested ternary expressions
    - Ternary in assignments
    - Ternary with complex conditions

---

## Implementation Order

**Test-driven approach, one feature at a time:**

1. **Feature 1 - Basic If Statement**
    - Foundation for all branching
    - Tests: Simple if with true/false conditions

2. **Feature 2 - If-Else Statement**
    - Extends Feature 1
    - Tests: If-else, nested, else-if chains

3. **Feature 3 - If as Expression**
    - Builds on Features 1-2
    - Tests: If-expression in assignments and contexts

4. **Feature 4 - Ternary Operator**
    - Independent conditional expression
    - Tests: Basic and nested ternary

## Critical Implementation Notes

### Type Safety

- Condition MUST be `Bool` type
- Throw `IllegalArgumentException` with clear message if not
- Apply to both if-statements and ternary

### Evaluation Tree ("Printable")

- Every evaluated node must capture:
    - The condition's evaluated value
    - Which branch was taken
    - The executed branch's evaluation tree
- This enables Epic 6's output formatting

### Grammar Precedence

- Ternary operator must be lowest precedence
- Use `<assoc=right>` for right-associativity
- Block vs single-statement handled by `block` rule

### Scoping (Deferred)

- Variables leak out of if blocks (flat scope)
- Document as known limitation
- Epic 3 will introduce proper lexical scoping

### Edge Cases to Test

- Empty if/else blocks
- If without else in expression context (what value?)
- Multiple statements in blocks (which value returned?)
- If condition with side effects (assignments in conditions)

## Definition of Done

- [ ] All grammar changes implemented and tested
- [ ] All AST node types added (Statement.IfStatement, Expression.TernaryOperator)
- [ ] All evaluated node types added (with proper "printable" structure)
- [ ] Parser visitor methods implemented
- [ ] Runtime evaluation for if-statement and ternary
- [ ] Type checking: conditions must be Bool
- [ ] All test cases passing:
    - Basic if (true/false conditions)
    - If-else (both branches)
    - Nested if/if-else
    - Else-if chains
    - If as expression (in assignments)
    - Ternary operator (basic and nested)
- [ ] ANTLR generation runs cleanly
- [ ] Full build passes (`./gradlew build`)

## Questions for Review

1. **If-expression without else**: What value should `if (false) { 5 }` return?
    - Option A: Throw error (else required for expressions)
    - Option B: Return null/unit value
    - **Recommendation**: Start with Option B, evaluate if this causes issues

2. **Multiple statements in if-block**: Should `if (true) { x = 5; 10; }` return 10 or 5?
    - **Recommendation**: Return last expression's value (10), matching Rust/Kotlin semantics

3. **Statement vs Expression**: Should we unify the hierarchy?
    - Current: `Statement` and `Expression` are separate
    - If-as-expression blurs this line
    - **Recommendation**: Keep separate for now, evaluate in Epic 3 (scoping) if unification needed

---

**Ready for implementation? Please review and confirm approach.**
