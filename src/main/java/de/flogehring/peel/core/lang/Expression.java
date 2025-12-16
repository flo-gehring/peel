package de.flogehring.peel.core.lang;

import de.flogehring.peel.core.values.PeelValue;

import java.util.List;
import java.util.Optional;

public sealed interface Expression {

    record Block(List<Expression> codeElements) implements Expression {
    }

    record Assignment(String variableName, Expression assignment) implements Expression {
    }


    record IfElseStatement(
            List<ConditionalExecution> conditionals,
            Optional<Expression> elseExpr
    ) implements Expression {

        public record ConditionalExecution(Expression condition, Expression then) {

        }
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

    record VariableName(String name) implements Expression {
        public VariableName {
            Expression.assertNotEmpty(name);
        }
    }

    record FunctionCall(String functionName, List<Expression> arguments) implements Expression {
        public FunctionCall {
            Expression.assertNotEmpty(functionName);
        }
    }

    record Loop(Expression condition, Block body) implements Expression {

    }

    private static void assertNotEmpty(String s) {
        assert s != null;
        assert !s.isEmpty();
    }
}