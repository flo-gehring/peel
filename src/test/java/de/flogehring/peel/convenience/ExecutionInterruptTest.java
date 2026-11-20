package de.flogehring.peel.convenience;

import de.flogehring.peel.core.trace.TraceValue;
import de.flogehring.peel.parse.PeelGrammar;
import de.flogehring.peel.run.exceptions.PeelInterruptedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionInterruptTest {
    @Test
    void interruptsAnActiveWhileLoop() throws InterruptedException {
        assertInterrupts("while (1 == 1) {}", "runLoop");
    }

    @Test
    void interruptsInsideAnActiveBuiltin() throws InterruptedException {
        assertInterrupts("count(\"text\", \"\");", "countSubstring");
    }

    private static void assertInterrupts(String script, String methodPrefix) throws InterruptedException {
        var runtime = RuntimeFactory.defaultLanguage();
        var program = PeelGrammar.parse(script);
        var outcome = new AtomicReference<Throwable>();
        var flagPreserved = new AtomicBoolean();
        var worker = Thread.ofPlatform().daemon().start(() -> {
            try {
                runtime.run(program);
            } catch (Throwable exception) {
                outcome.set(exception);
                flagPreserved.set(Thread.currentThread().isInterrupted());
            }
        });
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
            boolean entered = false;
            while (worker.isAlive() && System.nanoTime() < deadline) {
                entered = Arrays.stream(worker.getStackTrace())
                        .anyMatch(frame -> frame.getMethodName().startsWith(methodPrefix));
                if (entered) break;
                Thread.sleep(1);
            }
            assertThat(entered).as("script entered %s, outcome: %s", methodPrefix, outcome.get()).isTrue();
        } finally {
            worker.interrupt();
            worker.join(Duration.ofSeconds(2));
        }

        assertThat(worker.isAlive()).isFalse();
        assertThat(outcome.get()).isInstanceOf(PeelInterruptedException.class);
        assertThat(flagPreserved).isTrue();
        assertThat(runtime.run(PeelGrammar.parse("2 + 3;")).result()).isEqualTo(new TraceValue.IntegerValue(5));
    }
}
