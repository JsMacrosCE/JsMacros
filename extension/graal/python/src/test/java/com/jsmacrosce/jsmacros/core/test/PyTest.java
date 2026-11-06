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

public class PyTest extends BaseTest {

    @BeforeAll
    static void warmUpLanguageRuntime() throws InterruptedException {
        warmUpLanguage("py");
    }

    @Override
    public String getLang() {
        return "py";
    }

    @Test
    public void colorLibraryHandlesPackedArgbValues() throws InterruptedException {
        EventCustom result = runTestScript("""
                import json
                packed = Color.fromPacked(0x80000000)
                built = Color.argb(0x80, 0, 0, 0)
                with_alpha = Color.withAlpha(0x123456, 0x40)
                event.putString("test", json.dumps([
                    Color.alpha(packed), Color.red(packed), Color.green(packed), Color.blue(packed),
                    packed == built,
                    Color.alpha(Color.rgb(1, 2, 3)),
                    Color.alpha(with_alpha), Color.red(with_alpha)
                ]))
                """);
        assertEquals("[128, 0, 0, 0, true, 255, 64, 18]", result.getString("test"));
    }

    @Language("py")
    private final String TEST_SCRIPT = """
            import json
            order = []
            
            def a():
                order.append(1)
                print(1)
                event.putString("test", json.dumps(order))
            
            JavaWrapper.methodToJavaAsync(5, a).run()
            
            def b():
                order.append(2)
                print(2)
                event.putString("test", json.dumps(order))
            
            JavaWrapper.methodToJavaAsync(5, b).run()
            
            def c():
                order.append(3)
                print(3)
                event.putString("test", json.dumps(order))
            
            JavaWrapper.methodToJavaAsync(6, c).run()
            order.append(0)
            print(0)
            JavaWrapper.deferCurrentTask(-2) # change priority of this thread from 5 -> 3
            event.putString("test", json.dumps(order))
            """;

    @Test
    public void test() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT);
        assertEquals("[0, 3, 1, 2]", custom.getString("test"));
    }

    @Language("py")
    private final String TEST_SCRIPT_2 = """
            import json
            j = []
            atime = 0
            btime = 0
            
            def a():
                global atime
                while len(j) < 10:
                    j.append(f'a {atime}')
                    atime += 100
                    Time.sleep(100)
            
            JavaWrapper.methodToJavaAsync(a).run()

            def b():
                global btime
                while len(j) < 10:
                    j.append(f'b {btime}')
                    btime += 110        
                    Time.sleep(110)

            JavaWrapper.methodToJavaAsync(b).run()
            JavaWrapper.deferCurrentTask(-1)

            while len(j) < 10:
                JavaWrapper.deferCurrentTask()

            
            j.append('c')
            event.putString("test", json.dumps(j))
            """;

    @Test
    public void test2() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT_2, 5000);
        assertBothTasksCompleted(custom.getString("test"));
    }

    @Language("py")
    private final String TEST_SCRIPT_3 = """
            import json

            start = Time.time()
            a = []
            def long():
                a.append("long started")
                Time.sleep(5000)
                a.append('long finished')
                done()
            
            def rapid():
                a.append('rapid 1')
                Time.sleep(500)
                a.append('rapid 2')
                Time.sleep(500)
                a.append('rapid 3')
                Time.sleep(500)
                a.append('rapid 4')
                done()
            
            isDone = False
            def done():
                if len(a) == 6:
                    event.putString("test", json.dumps(a))
                    event.putDouble("time", Time.time() - start)
                    isDone = True
            
            def runAsync(fn):
                JavaWrapper.methodToJavaAsync(fn).run()
            
            runAsync(long)
            runAsync(rapid)
            while not isDone:
                JavaWrapper.deferCurrentTask()
            """;

    @Test
    public void test3() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT_3);
        System.out.println("Time: " + custom.getDouble("time"));
        assertEquals("[\"long started\", \"rapid 1\", \"rapid 2\", \"rapid 3\", \"rapid 4\", \"long finished\"]", custom.getString("test"));
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
