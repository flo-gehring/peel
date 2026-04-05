package de.flogehring.peel.convenience;

import de.flogehring.peel.core.eval.ArithmeticConfiguration;
import de.flogehring.peel.core.eval.ArithmeticPolicy;
import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.values.Number;

import java.util.List;

public final class ArithmeticPolicies {

    private ArithmeticPolicies() {
    }

    public static ArithmeticPolicy standard() {
        return ArithmeticPolicyDefaults.standard();
    }

    public static ArithmeticPolicy fast() {
        return ArithmeticPolicyDefaults.fast();
    }

    public static ArithmeticPolicy financial() {
        return ArithmeticPolicyDefaults.financial();
    }

    public static ArithmeticConfiguration.Builder configure() {
        return ArithmeticConfiguration.init();
    }

    public static List<OperatorDef> operatorDefinitions(ArithmeticPolicy policy) {
        return List.of(
                OperatorDef.arithmeticManaged(
                        "+",
                        Number.class,
                        Number.class,
                        (lhs, rhs) -> policy.add((Number) lhs, (Number) rhs)
                ),
                OperatorDef.arithmeticManaged(
                        "-",
                        Number.class,
                        Number.class,
                        (lhs, rhs) -> policy.sub((Number) lhs, (Number) rhs)
                ),
                OperatorDef.arithmeticManaged(
                        "*",
                        Number.class,
                        Number.class,
                        (lhs, rhs) -> policy.mul((Number) lhs, (Number) rhs)
                ),
                OperatorDef.arithmeticManaged(
                        "%",
                        Number.class,
                        Number.class,
                        (lhs, rhs) -> policy.mod((Number) lhs, (Number) rhs)
                ),
                OperatorDef.arithmeticManaged(
                        "**",
                        Number.class,
                        Number.class,
                        (lhs, rhs) -> policy.pow((Number) lhs, (Number) rhs)
                ),
                OperatorDef.arithmeticManaged(
                        "/",
                        Number.class,
                        Number.class,
                        (lhs, rhs) -> policy.div((Number) lhs, (Number) rhs)
                ),
                OperatorDef.arithmeticManaged(
                        "-",
                        Number.class,
                        value -> policy.negate((Number) value)
                )
        );
    }
}
