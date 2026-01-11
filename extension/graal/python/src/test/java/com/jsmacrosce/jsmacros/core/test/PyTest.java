package com.jsmacrosce.jsmacros.core.test;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.test.BaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PyTest extends BaseTest {

    @Override
    public String getLang() {
        return "py";
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
        assertEquals("[\"a 0\", \"b 0\", \"a 100\", \"b 110\", \"a 200\", \"b 220\", \"a 300\", \"b 330\", \"a 400\", \"b 440\", \"c\"]", custom.getString("test"));
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

}
