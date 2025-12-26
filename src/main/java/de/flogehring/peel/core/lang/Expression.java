package de.flogehring.peel.core.lang;

import de.flogehring.peel.core.values.PeelValue;

import java.util.List;
import java.util.Optional;

public sealed interface Expression {

    record Block(List<Expression> codeElements) implements Expression {
    }

    record Assignment(String variableName, Expression assignment, int scopeOffset) implements Expression {
    }

    record IfElseStatement(
            List<ConditionalExecution> conditionals,
            Optional<Expression> elseExpr
    ) implements Expression {

        public record ConditionalExecution(Expression condition, Expression then) {

        }
    }

    record ListLiteral(List<Expression> elements) implements Expression {

    }

    record Literal(PeelValue value) implements Expression {
    }

    record BinaryOperator(
            String operator,
            Expression lhs,
            Expression rhs
    ) implements Expression {
        public BinaryOperator {
            Expression.assertNotEmpty(operator);
        }
    }

    record UnaryPrefixOperator(
            String operator,
            Expression arg
    ) implements Expression {
    }

    record VariableName(String name, int scopeOffset) implements Expression {
        public VariableName {
            Expression.assertNotEmpty(name);
        }
    }

    record FunctionCall(Expression functionName, List<Expression> arguments) implements Expression {
    }

    record WhileLoop(Expression condition, Block body) implements Expression {

    }

    record ForEachLoop(String varName, Expression list, Block body) implements Expression {

    }

    private static void assertNotEmpty(String s) {
        assert s != null;
        assert !s.isEmpty();
    }
}