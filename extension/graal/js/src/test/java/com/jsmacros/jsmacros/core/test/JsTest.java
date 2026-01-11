package com.jsmacrosce.jsmacros.core.test;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.test.BaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsTest extends BaseTest {

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
        assertEquals("[\"a 0\",\"b 0\",\"a 100\",\"b 110\",\"a 200\",\"b 220\",\"a 300\",\"b 330\",\"a 400\",\"b 440\",\"c\"]", custom.getString("test"));
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

}
