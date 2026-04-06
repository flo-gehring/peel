package de.flogehring.peel.run;

import de.flogehring.peel.convenience.RuntimeBuilder;
import de.flogehring.peel.core.eval.ArithmeticPolicy;
import de.flogehring.peel.core.eval.OperatorDef;
import de.flogehring.peel.core.eval.PeelRuntime;
import de.flogehring.peel.core.lang.Program;
import de.flogehring.peel.core.trace.TraceProgram;
import de.flogehring.peel.core.trace.TraceValueMapper;
import de.flogehring.peel.core.values.Number;
import de.flogehring.peel.core.values.PeelValue;
import de.flogehring.peel.core.values.Text;
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peel.run.exceptions.PeelException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ExtensibilityTest {

    private static void runProgrammAndExpect(PeelRuntime runtime, String text, PeelValue expected) {
        Program program = PeelGrammar.parse(text);
        TraceProgram trace = runtime.run(program);
        assertThat(trace.result()).isEqualTo(TraceValueMapper.fromPeelValue(expected));
    }

    private static TraceProgram runProgramm(PeelRuntime runtime, String text) {
        Program program = PeelGrammar.parse(text);
        return runtime.run(program);
    }

    private static Number.Decimal decimal(String value) {
        return new Number.Decimal(new BigDecimal(value));
    }

    @Nested
    class CustomArithmetic {

        private final PeelRuntime runtime = RuntimeBuilder.standardLanguage()
                .withArithmetic(customRoundedDecimalArithmetic())
                .build();

        @Test
        void roundsDownForFourthDecimalSeven() {
            runProgrammAndExpect(runtime, "100.0047 + 0;", decimal("100.004"));
        }

        @Test
        void roundsUpForFourthDecimalEight() {
            runProgrammAndExpect(runtime, "100.0048 + 0;", decimal("100.005"));
        }

        @Test
        void allArithmeticResultsAreDecimals() {
            runProgrammAndExpect(runtime, "1 + 2;", decimal("3.000"));
        }

        @Test
        void appliesCustomRoundingAfterOperation() {
            runProgrammAndExpect(runtime, "50.0024 + 50.0024;", decimal("100.005"));
        }

        @Test
        void keepsRoundingRuleForNegativeNumbers() {
            runProgrammAndExpect(runtime, "-100.0048 + 0;", decimal("-100.005"));
        }

        @Test
        void supportsPowerAndModuloUnderCustomPolicy() {
            runProgrammAndExpect(runtime, "(2 ** 3) + (7 % 2);", decimal("9.000"));
        }

        @Test
        void divisionResultIsRoundedToThreeDecimals() {
            runProgrammAndExpect(runtime, "1 / 6;", decimal("0.166"));
        }
    }

    @Nested
    class CustomUnaryOperator {

        private final PeelRuntime runtime = RuntimeBuilder.standardLanguage()
                .withOperator(OperatorDef.typed("!", Number.Integer.class, value -> factorial((Number.Integer) value)))
                .build();

        @Test
        void factorialOfFive() {
            runProgrammAndExpect(runtime, "!5;", PeelValue.integer(120));
        }

        @Test
        void factorialOfZero() {
            runProgrammAndExpect(runtime, "!0;", PeelValue.integer(1));
        }

        @Test
        void factorialWorksInExpressions() {
            runProgrammAndExpect(runtime, "!3 + !4;", PeelValue.integer(30));
        }

        @Test
        void factorialWithNegationPrefix() {
            runProgrammAndExpect(runtime, "-!3 + -!4;", PeelValue.integer(-30));
        }

        @Test
        void boolNotStillWorksFromStandardModule() {
            runProgrammAndExpect(runtime, "!False;", PeelValue.bool(true));
        }

        private PeelValue factorial(Number.Integer value) {
            int n = value.value();
            if (n < 0) {
                throw new PeelException("Factorial only defined for non-negative integers");
            }
            int result = 1;
            for (int i = 2; i <= n; i++) {
                result *= i;
            }
            return PeelValue.integer(result);
        }
    }

    @Nested
    class CustomFunctionsPigLatin {

        private final PeelRuntime runtime = RuntimeBuilder.standardLanguage()
                .withFunction(new SimpleFunction(
                        "pigLatinWord",
                        1,
                        arguments -> pigLatinWord(arguments[0])
                ))
                .withFunction(new SimpleFunction(
                        "pigLatinSentence",
                        1,
                        arguments -> pigLatinSentence(arguments[0])
                ))
                .build();

        @Test
        void pigLatinWordForConsonantStart() {
            runProgrammAndExpect(runtime, "pigLatinWord(\"pig\");", PeelValue.text("igpay"));
            runProgrammAndExpect(runtime, "pigLatinWord(\"latin\");", PeelValue.text("atinlay"));
            runProgrammAndExpect(runtime, "pigLatinWord(\"banana\");", PeelValue.text("ananabay"));
            runProgrammAndExpect(runtime, "pigLatinWord(\"black\");", PeelValue.text("ackblay"));
        }

        @Test
        void pigLatinWordForVowelStart() {
            runProgrammAndExpect(runtime, "pigLatinWord(\"a\");", PeelValue.text("away"));
            runProgrammAndExpect(runtime, "pigLatinWord(\"open\");", PeelValue.text("openway"));
        }

        @Test
        void pigLatinSentenceUsesWordFunctionForEachWord() {
            runProgrammAndExpect(
                    runtime,
                    "pigLatinSentence(\"pig latin banana black\");",
                    PeelValue.text("igpay atinlay ananabay ackblay")
            );
        }

        @Test
        void pigLatinWordThrowsOnWhitespace() {
            assertThatExceptionOfType(PeelException.class).isThrownBy(
                    () -> runProgramm(runtime, "pigLatinWord(\"pig latin\");")
            );
        }

        private PeelValue pigLatinWord(PeelValue value) {
            String word = requireText(value);
            if (word.chars().anyMatch(Character::isWhitespace)) {
                throw new PeelException("pigLatinWord expects a single word without whitespace");
            }
            return PeelValue.text(toPigLatinWord(word));
        }

        private PeelValue pigLatinSentence(PeelValue value) {
            String sentence = requireText(value);
            if (sentence.isBlank()) {
                return PeelValue.text("");
            }
            String translated = Arrays.stream(sentence.trim().split("\\s+"))
                    .map(this::toPigLatinWord)
                    .collect(Collectors.joining(" "));
            return PeelValue.text(translated);
        }

        private String requireText(PeelValue value) {
            if (value instanceof Text(String text)) {
                return text;
            }
            throw new PeelException("Expected Text argument");
        }

        private String toPigLatinWord(String word) {
            int firstVowel = firstVowelIndex(word);
            if (firstVowel == 0) {
                return word + "way";
            }
            if (firstVowel < 0) {
                return word + "ay";
            }
            return word.substring(firstVowel) + word.substring(0, firstVowel) + "ay";
        }

        private int firstVowelIndex(String word) {
            for (int i = 0; i < word.length(); i++) {
                char c = Character.toLowerCase(word.charAt(i));
                if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') {
                    return i;
                }
            }
            return -1;
        }
    }

    private static ArithmeticPolicy customRoundedDecimalArithmetic() {
        return new ArithmeticPolicy() {
            @Override
            public PeelValue add(Number lhs, Number rhs) {
                return round(lhs.numberValue().add(rhs.numberValue()));
            }

            @Override
            public PeelValue sub(Number lhs, Number rhs) {
                return round(lhs.numberValue().subtract(rhs.numberValue()));
            }

            @Override
            public PeelValue mul(Number lhs, Number rhs) {
                return round(lhs.numberValue().multiply(rhs.numberValue()));
            }

            @Override
            public PeelValue mod(Number lhs, Number rhs) {
                if (rhs.numberValue().compareTo(BigDecimal.ZERO) == 0) {
                    throw new ArithmeticException("Modulo by zero");
                }
                return round(lhs.numberValue().remainder(rhs.numberValue()));
            }

            @Override
            public PeelValue pow(Number lhs, Number rhs) {
                double value = Math.pow(lhs.numberValue().doubleValue(), rhs.numberValue().doubleValue());
                return round(BigDecimal.valueOf(value));
            }

            @Override
            public PeelValue div(Number lhs, Number rhs) {
                if (rhs.numberValue().compareTo(BigDecimal.ZERO) == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                BigDecimal quotient = lhs.numberValue().divide(rhs.numberValue(), 16, RoundingMode.HALF_UP);
                return round(quotient);
            }

            @Override
            public PeelValue negate(Number value) {
                return round(value.numberValue().negate());
            }

            private PeelValue round(BigDecimal value) {
                return new Number.Decimal(roundToThreeDigitsWithEightThreshold(value));
            }

            private BigDecimal roundToThreeDigitsWithEightThreshold(BigDecimal value) {
                BigDecimal truncated = value.setScale(3, RoundingMode.DOWN);
                BigDecimal shifted = value.abs().movePointRight(3);
                BigDecimal fractionalPart = shifted.remainder(BigDecimal.ONE);
                if (fractionalPart.compareTo(new BigDecimal("0.8")) >= 0) {
                    BigDecimal step = new BigDecimal("0.001");
                    if (value.signum() < 0) {
                        return truncated.subtract(step);
                    }
                    return truncated.add(step);
                }
                return truncated;
            }
        };
    }
}
