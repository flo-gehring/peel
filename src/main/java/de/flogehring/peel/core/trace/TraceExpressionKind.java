package de.flogehring.peel.core.trace;

public enum TraceExpressionKind {
    LITERAL,
    BINARY_OPERATOR,
    UNARY_PREFIX_OPERATOR,
    VARIABLE_NAME,
    FUNCTION_CALL,
    RETURN_EXPR,
    ASSIGNMENT,
    IF_STATEMENT,
    WHILE_LOOP,
    FOR_EACH_LOOP,
    LIST_LITERAL,
    MAP_LITERAL,
    SELECTOR,
    BLOCK
}
