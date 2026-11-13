package com.jsmacrosce.jsmacros.core.test;

import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.test.BaseTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the JavaScript language and the deferred-task scheduling shared by the Graal extensions.
 *
 * <p>A callback submitted with {@code JavaWrapper.methodToJavaAsync(...).run()} is handed to a
 * pool worker and joins the script queue from that worker. Two submissions from the same script
 * can therefore join in either order. The tests below do not assert that order; where one task
 * must start first, the script waits for it to run before submitting the next.
 */
public class JsTest extends BaseTest {

    private static final int TIMEOUT_MS = 15_000;

    @BeforeAll
    static void warmUpLanguageRuntime() throws InterruptedException {
        warmUpLanguage("js");
    }

    @com.jsmacrosce.jsmacros.core.library.ScriptNamespace("IntegrationTypes")
    public static final class TypeCatalog {
        public static final Class<StringBuilder> Builder = StringBuilder.class;
    }

    @Test
    public void scriptExecutionInstallsTypeNamespaces() throws InterruptedException {
        com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator.core.scriptTypeRegistry.register(TypeCatalog.class);
        EventCustom result = runTestScript("""
                const text = new IntegrationTypes.Builder('exports');
                event.putString('test', text instanceof IntegrationTypes.Builder ? text.toString() : 'wrong type');
                """);
        assertEquals("exports", result.getString("test"));
    }

    @Test
    public void colorLibraryHandlesPackedArgbValues() throws InterruptedException {
        EventCustom result = runTestScript("""
                const packed = Color.fromPacked(0x80000000);
                const built = Color.argb(0x80, 0, 0, 0);
                const withAlpha = Color.withAlpha(0x123456, 0x40);
                let rejected = false;
                try {
                    Color.fromPacked(0x100000000);
                } catch (error) {
                    rejected = true;
                }
                event.putString('test', [
                    Color.alpha(packed), Color.red(packed), Color.green(packed), Color.blue(packed),
                    packed === built,
                    Color.alpha(Color.rgb(1, 2, 3)),
                    Color.alpha(withAlpha), Color.red(withAlpha),
                    typeof Color.fixAlpha, rejected
                ].join(','));
                """);
        assertEquals("128,0,0,0,true,255,64,18,undefined,true", result.getString("test"));
    }

    @Override
    public String getLang() {
        return "js";
    }

    @Language("js")
    private final String PRIORITY_SCRIPT = """
            var order = []
            JavaWrapper.methodToJavaAsync(5, () => {
                order.push(1);
            }).run();
            JavaWrapper.methodToJavaAsync(5, () => {
                order.push(2);
            }).run();
            JavaWrapper.methodToJavaAsync(6, () => {
                order.push(3);
            }).run();
            order.push(0);
            // Lower this thread from 5 to 3 so all three tasks run before it writes the result.
            JavaWrapper.deferCurrentTask(-2)
            while (order.length < 4) {
                JavaWrapper.deferCurrentTask()
            }
            event.putString("test", JSON.stringify(order));
            """;

    /**
     * Checks that a deferred task's priority decides when it runs, and that the running thread can
     * lower its own priority before yielding.
     *
     * <p>The priority 6 task must run before both priority 5 tasks. The main thread lowers itself
     * to priority 3 before yielding and writes the result only after all three tasks have run. The
     * two priority 5 tasks are submitted back to back but join the queue from separate pool
     * workers, so their order is not specified and the test accepts either.
     */
    @Test
    public void deferredTasksRunInPriorityOrder() throws InterruptedException {
        EventCustom result = runTestScript(PRIORITY_SCRIPT, TIMEOUT_MS);
        assertPriorityOrder(result.getString("test"));
    }

    @Language("js")
    private final String TWO_TASKS_SCRIPT = """
            var j = []
            JavaWrapper.methodToJavaAsync(() => {
                for (var i = 0; i < 5; i++) {
                    j.push(`a ${i * 100}`);
                    Time.sleep(100);
                }
            }).run();
            JavaWrapper.methodToJavaAsync(() => {
                for (var i = 0; i < 5; i++) {
                    j.push(`b ${i * 110}`);
                    Time.sleep(110);
                }
            }).run();
            JavaWrapper.deferCurrentTask(-1);
            while (j.length < 10) {
                JavaWrapper.deferCurrentTask();
            }
            j.push('c');
            event.putString("test", JSON.stringify(j));
            """;

    /**
     * Checks that two deferred tasks each run to completion while the main thread yields, and that
     * the main thread resumes only after both are done.
     *
     * <p>The tasks sleep for different durations, so how their entries interleave changes between
     * runs. The test asserts each task's own sequence and the main thread's marker at the end; it
     * does not assert any particular interleaving.
     */
    @Test
    public void deferredTasksRunToCompletionBeforeMainResumes() throws InterruptedException {
        EventCustom result = runTestScript(TWO_TASKS_SCRIPT, TIMEOUT_MS);
        assertEachTaskCompleted(result.getString("test"));
    }

    @Language("js")
    private final String SLEEP_YIELD_SCRIPT = """
            const start = Time.time();
            let a = [];
            function long() {
                a.push("long started");
                Time.sleep(5000);
                a.push("long finished");
            }
            function rapid() {
                for (let i = 1; i <= 4; i++) {
                    a.push(`rapid ${i}`);
                    Time.sleep(250);
                }
            }
            function runAsync(fn) {
                JavaWrapper.methodToJavaAsync(fn).run();
            }
            runAsync(long);
            // Two submissions can join the queue in either order, so wait until the long task
            // has announced itself before submitting the rapid one.
            while (a.length == 0) {
                JavaWrapper.deferCurrentTask();
            }
            runAsync(rapid);
            while (a.length < 6) {
                JavaWrapper.deferCurrentTask();
            }
            event.putString("test", JSON.stringify(a));
            event.putDouble("time", Time.time() - start);
            """;

    /**
     * Checks that sleeping in one deferred task yields the runtime instead of blocking it.
     *
     * <p>The long task announces itself and then sleeps for five seconds. The rapid task appends
     * four entries a quarter of a second apart. Seeing all four rapid entries before
     * {@code "long finished"} can only happen if the sleep released the runtime. The rapid task is
     * submitted only after the long one has run, because two submissions are not ordered.
     */
    @Test
    public void sleepingTaskYieldsToOtherDeferredTasks() throws InterruptedException {
        EventCustom result = runTestScript(SLEEP_YIELD_SCRIPT, TIMEOUT_MS);
        assertEquals(
                "[\"long started\",\"rapid 1\",\"rapid 2\",\"rapid 3\",\"rapid 4\",\"long finished\"]",
                result.getString("test")
        );
        assertTrue(result.getDouble("time") >= 5000,
                "the long task must sleep for five seconds: " + result.getDouble("time"));
    }

    /**
     * Splits the JSON array the scheduling scripts write into its entries.
     *
     * <p>The entries never contain a comma, so splitting is enough and avoids pulling a JSON
     * parser into the test.
     */
    private static List<String> entries(String json) {
        String body = json.trim();
        body = body.substring(1, body.length() - 1);
        List<String> entries = new ArrayList<>();
        for (String part : body.split(",")) {
            entries.add(part.trim().replace("\"", ""));
        }
        return entries;
    }

    /**
     * Asserts that both tasks appended their own sequence and that the main thread resumed last.
     *
     * <p>The entries of the two tasks may interleave in any order; only each task's sequence and
     * the main thread's marker are guaranteed.
     */
    private static void assertEachTaskCompleted(String json) {
        List<String> entries = entries(json);
        assertEquals(11, entries.size(), entries.toString());
        assertEquals("c", entries.get(entries.size() - 1), "the main thread should resume after both tasks");
        assertEquals(List.of("a 0", "a 100", "a 200", "a 300", "a 400"),
                entries.stream().filter(e -> e.startsWith("a ")).toList());
        assertEquals(List.of("b 0", "b 110", "b 220", "b 330", "b 440"),
                entries.stream().filter(e -> e.startsWith("b ")).toList());
    }

    /**
     * Asserts the only order the priority test guarantees: the main thread's own entry first, the
     * priority 6 task next, and the two priority 5 tasks last in either order.
     */
    private static void assertPriorityOrder(String json) {
        List<String> order = entries(json);
        assertEquals(4, order.size(), order.toString());
        assertEquals("0", order.get(0), "the main thread should append before yielding");
        assertEquals("3", order.get(1), "the priority 6 task should run before the priority 5 tasks");
        assertEquals(Set.of("1", "2"), Set.copyOf(order.subList(2, 4)),
                "the priority 5 tasks have no defined order: " + order);
    }

}
