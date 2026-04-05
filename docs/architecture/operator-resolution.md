# Operator Resolution

This document defines how PEEL resolves operators at runtime.

## Scope

- Applies to unary and binary operators.
- Applies to typed `OperatorDef` registrations.
- Function calls (`name(args...)`) are resolved separately (by function name + arity) and are not part of operator
  resolution.

## High-Level Flow

Given an operator symbol and runtime operands, the resolver:

1. Collects registered operator definitions for the symbol.
2. Filters to definitions with matching arity and compatible operand types.
3. Computes a specificity score for each compatible definition.
4. Selects the highest-scoring definition.
5. If multiple definitions tie for best score, throws `AmbiguousOperatorException`.
6. If none match, throws `NoFunctionFoundException`.

Implementation entrypoint: `src/main/java/de/flogehring/peel/run/OperatorResolver.java`.

## Type Model (Formalized)

Resolution no longer depends on generic Java class-graph distance.
It uses a PEEL-specific type hierarchy (`PeelValueKind`):

- `VALUE`
- `PRIMITIVE` -> (`NUMBER`, `TEXT`, `BOOL`, `NONE`)
- `NUMBER` -> (`INTEGER`, `FLOAT`, `DECIMAL`)
- `COLLECTION` -> (`LIST`, `MAP`)
- `CALLABLE`

Implementation: `src/main/java/de/flogehring/peel/run/PeelValueKind.java`.

### Kind Mapping

- Runtime operands are mapped from concrete `PeelValue` instances to `PeelValueKind`.
- Declared operator argument types (`Class<? extends PeelValue>`) are mapped to a declared `PeelValueKind`.
- Only supported PEEL value classes/families are valid in typed operator signatures.

## Matching Rules

Declared kind matches actual kind when:

- They are identical, or
- Declared kind is a parent family kind:
    - `VALUE` matches everything
    - `PRIMITIVE` matches `NUMBER`, `INTEGER`, `FLOAT`, `DECIMAL`, `TEXT`, `BOOL`, `NONE`
    - `NUMBER` matches `INTEGER`, `FLOAT`, `DECIMAL`
    - `COLLECTION` matches `LIST`, `MAP`

Otherwise, it does not match.

Unary and binary resolution are independent:

- Unary call only checks unary definitions (`arity == 1`).
- Binary call only checks binary definitions (`arity == 2`).

## Specificity Scoring

Current scoring contract:

- Exact match: very high score (`10000`)
- Family match (`PRIMITIVE`, `NUMBER`, `COLLECTION`): medium score (`5000`)
- Catch-all `VALUE`: low score (`1`)
- Non-match: invalid (`Integer.MIN_VALUE`)

For binary operators, total score is `lhsScore + rhsScore`.
Highest total score wins.

If multiple candidates have equal highest score, resolution is ambiguous and fails fast.

## Determinism

Candidates are sorted before ambiguity reporting to ensure deterministic diagnostics:

- Unary: sort by declared `lhsType` name.
- Binary: sort by `lhsType|rhsType` name.

This does not break ties; it only makes error output stable.

## Error Behavior

- No compatible operator: `NoFunctionFoundException`
- Multiple equally specific compatible operators: `AmbiguousOperatorException`

## Special Language Operators

`&&` and `||` are special language-level operators with short-circuit semantics:

- They are evaluated directly in `src/main/java/de/flogehring/peel/run/Evaluator.java`.
- They do not rely on the normal typed operator resolution pipeline for parsed expressions.
- Right-hand side evaluation is conditional (`&&` skips RHS when LHS is `false`; `||` skips RHS when LHS is `true`).

Because of this contract, overriding these symbols via `RuntimeBuilder` is blocked at build time.
Attempting to register `&&` or `||` through modules or explicit operator registrations throws
`ReservedOperatorOverrideException`.

## Numeric Override Validation at Build Time

`RuntimeBuilder` enforces a guardrail for numeric operator overrides:

- By default, registering non-arithmetic-managed binary numeric operators (`Number x Number`) is blocked.
- Exception can be enabled explicitly via `allowNumericOperatorOverrides()`.

Validation location: `src/main/java/de/flogehring/peel/convenience/RuntimeBuilder.java`.

### Why This Changed Some Test Assumptions

During refactor, some tests initially assumed they could register broad numeric operators using common built-in
symbols (for example `+`) without additional configuration.

After build-time validation was centralized, two things became explicit:

1. Numeric overrides require opt-in (`allowNumericOperatorOverrides()`).
2. Using common arithmetic symbols in override tests can unintentionally collide with default arithmetic operators,
   leading to ambiguity when broad signatures are used.

To keep tests focused and deterministic, those tests now use non-standard symbols (for example `~`) when testing
override behavior itself.

This isolates the test intent:

- test override guardrail behavior,
- not collision semantics with built-in arithmetic operators.

## Extension Guidance

- Prefer specific signatures (`INTEGER`, `TEXT`, etc.) over broad ones (`VALUE`) unless intentionally generic.
- Avoid registering overlapping broad signatures for the same symbol unless ambiguity is acceptable.
- Keep unary and binary operator APIs explicit; functions do not implicitly become operators.
