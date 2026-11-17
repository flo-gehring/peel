# Language analysis for consumers

Parsing and optional AST analyses are independent. Obtain an executable program
with `PeelGrammar.parse(source)`. Only consumers needing block names, parent
relationships or possible reachability call `BlockCatalog.analyze(program)`.
Duplicate block names are rejected by that explicit analysis, not by parsing.
The editor analyzer independently reports them as diagnostics.

The core library offers an editor-independent analysis API:

```java
var inputs = List.of(new ProvidedSymbol("amount", SymbolKind.VARIABLE, "Net amount", -1));
var analysis = PeelAnalysis.analyze(
        "var gross = amount * 1.19;",
        inputs,
        RuntimeFactory.defaultSymbols()
);
```

The classes are in `de.flogehring.peel.analysis`. Analysis never evaluates code
or invokes registered callbacks. `RuntimeBuilder.symbols()` describes names and
arities from the same modules/functions/variables used to build the runtime.
Provided inputs need no values.

AnalysisResult contains definitions, occurrences, lexical scopes and diagnostics.
Symbol IDs connect occurrences to definitions. Kinds distinguish variables,
parameters and declared functions; origins distinguish local, external and
runtime symbols. Roles distinguish declarations, references, assignments and calls.
A variable holding a function remains a variable; there is no callable-value
inference. Runtime overload names share a definition for presentation.

The flat result uses enums: SymbolKind (VARIABLE, PARAMETER, FUNCTION),
SymbolOrigin (LOCAL, EXTERNAL, RUNTIME), OccurrenceRole (DECLARATION, REFERENCE,
ASSIGNMENT, CALL), DiagnosticCode and DiagnosticSeverity (WARNING, ERROR).
These fields are typed Java values; default JSON serialization uses their uppercase
enum names. Javadocs on the records describe every field, ID relationship and range.

Ranges use zero-based UTF-16 offsets with exclusive ends. Consumers should use
scope ancestry, ranges and visibleFrom positions to offer visible names.
`openEnded` scopes include the cursor at end-of-source for unfinished input and
the program scope. Complete closed nested scopes exclude that end position.
Inner declarations shadow outer definitions; future declarations are not hoisted.

Named and anonymous standalone blocks, named function/loop bodies and single
statement bodies use the interpreter's scopes. A block label is not a variable
or a completion candidate. Duplicate labels are diagnosed across the entire
script, including unexecuted branches and functions. Missing outer braces keep
the unfinished scope open even when its last inner block is already closed.

Empty and unfinished programs are accepted. Syntax errors are returned as
diagnostics, safe subtrees are analyzed, and unknown reads are warnings. Existing
illegal assignments, duplicate declarations and misplaced returns produce errors.
No host callbacks are used to infer types or validate runtime values.

`PeelAnalysis.isIdentifier(name)` validates a complete identifier using the
generated lexer, including keyword exclusion. Application-specific input
contracts, persistence, HTTP, themes and Monaco adapters belong to consumers.
