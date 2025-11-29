package de.flogehring.peel.core.lang;

import de.flogehring.peel.core.values.PeelValue;

import java.util.List;

public sealed interface Expression extends CodeElement {


    record Block(List<Expression> codeElements) implements Expression {

    }

    record Assignment(String variableName, Expression assignment) implements Expression {

    }

    record IfStatement(Expression condition, Expression.Block thenBlock,
                       Expression.Block elseBlock) implements Expression {

    }

    record Literal(PeelValue value) implements Expression {
    }

    record BinaryOperator(String operator, Expression lhs, Expression rhs) implements Expression {
        public BinaryOperator {
            Expression.assertNotEmpty(operator);
        }
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

    private static void assertNotEmpty(String s) {
        assert s != null;
        assert !s.isEmpty();
    }


}