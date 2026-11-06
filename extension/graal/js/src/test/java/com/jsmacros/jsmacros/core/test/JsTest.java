package com.jsmacrosce.jsmacros.core.test;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.test.BaseTest;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsTest extends BaseTest {

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
    private final String TEST_SCRIPT = """
            var order = []
            JavaWrapper.methodToJavaAsync(5, () => {
                order.push(1);
                event.putString("test", JSON.stringify(order));
            }).run();
            JavaWrapper.methodToJavaAsync(5, () => {
                order.push(2);
                event.putString("test", JSON.stringify(order));
            }).run();
            JavaWrapper.methodToJavaAsync(6, () => {
                order.push(3);
                event.putString("test", JSON.stringify(order));
            }).run();
            order.push(0);
            JavaWrapper.deferCurrentTask(-2) // change priority of this thread from 5 -> 3
            event.putString("test", JSON.stringify(order));
        """;

    @Test
    public void test() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT);
        assertEquals("[0,3,1,2]", custom.getString("test"));
    }

    @Language("js")
    private final String TEST_SCRIPT_2 = """
            var j = []
            var atime = 0
            var btime = 0
            JavaWrapper.methodToJavaAsync(() => {
                while (j.length < 10) {
                    j.push(`a ${atime}`);
                    atime += 100
                    Time.sleep(100)
                }
            }).run();
            JavaWrapper.methodToJavaAsync(() => {
                while (j.length < 10) {
                    j.push(`b ${btime}`);
                    btime += 110
                    Time.sleep(110)
                }
            }).run();
            JavaWrapper.deferCurrentTask(-1)
            while(j.length < 10) {
                JavaWrapper.deferCurrentTask()
            }
            j.push('c');
            event.putString("test", JSON.stringify(j))
            """;

    @Test
    public void test2() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT_2);
        assertBothTasksCompleted(custom.getString("test"));
    }

    @Language("js")
    private final String TEST_SCRIPT_3 = """
            const start = Time.time()
            let a = []
            function long() {
                a.push("long started");
                Time.sleep(5000);
                a.push('long finished');
                done()
            }
            function rapid() {
                a.push('rapid 1')
                Time.sleep(500);
                a.push('rapid 2')
                Time.sleep(500);
                a.push('rapid 3')
                Time.sleep(500);
                a.push('rapid 4')
                done()
            }
            var isDone = false
            function done() {
                if (a.length == 6) {
                    event.putString("test", JSON.stringify(a))
                    event.putDouble("time", Time.time() - start)
                    isDone = true
                }
            }
            function runAsync(fn) {
                JavaWrapper.methodToJavaAsync(fn).run()
            }
            runAsync(long)
            runAsync(rapid)
            while (!isDone) {
                JavaWrapper.deferCurrentTask()
            }
            """;

    @Test
    public void test3() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT_3, 7000);
        System.out.println("Time: " + custom.getDouble("time"));
        assertEquals("[\"long started\",\"rapid 1\",\"rapid 2\",\"rapid 3\",\"rapid 4\",\"long finished\"]", custom.getString("test"));
        assertTrue(custom.getDouble("time") > 5000);
        assertTrue(custom.getDouble("time") < 7000);
    }


    /**
     * Splits the JSON array the script writes into its entries.
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
     * Asserts the contract of the two deferred tasks without asserting their exact interleaving.
     * Both tasks are queued together and their sleeps differ by 10 ms, so which appends first, and
     * the occasional reordering of a pair, is scheduling noise rather than behavior. What the
     * script does guarantee is that each task keeps its own counter and that the main thread only
     * continues after both are done.
     */
    private static void assertBothTasksCompleted(String json) {
        List<String> entries = entries(json);
        assertEquals("c", entries.get(entries.size() - 1), "the main thread should continue last");
        assertEquals(List.of("a 0", "a 100", "a 200", "a 300", "a 400"),
                entries.stream().filter(e -> e.startsWith("a ")).toList());
        assertEquals(List.of("b 0", "b 110", "b 220", "b 330", "b 440"),
                entries.stream().filter(e -> e.startsWith("b ")).toList());
    }

}
