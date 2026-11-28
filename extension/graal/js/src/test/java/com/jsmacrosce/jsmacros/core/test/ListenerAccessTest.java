package com.jsmacrosce.jsmacros.core.test;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventListener;
import com.jsmacrosce.jsmacros.core.event.IEventListener;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.core.library.impl.FJsMacros;
import com.jsmacrosce.jsmacros.test.BaseTest;
import com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator;
import com.jsmacrosce.jsmacros.test.stubs.EventRegistryStub;
import com.jsmacrosce.jsmacros.test.stubs.ProfileStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the two ways to reach listeners from a script: the script-created listeners returned by
 * {@code JsMacros.listeners(...)}, and the profile macros reported by {@code JsMacros.macroTriggers()}.
 * Macros are identified by their trigger because a script does not need the listener wrapper.
 */
class ListenerAccessTest extends BaseTest {

    private static final String EVENT = "ListenerAccessTestTick";

    @TempDir
    Path tempDir;

    @BeforeAll
    static void warmUpLanguageRuntime() throws InterruptedException {
        warmUpLanguage("js");
    }

    private static Core<ProfileStub, EventRegistryStub> core() {
        return CoreInstanceCreator.core;
    }

    @Override
    public String getLang() {
        return "js";
    }

    @BeforeEach
    void clearRegistryBeforeTest() {
        clearRegistry();
    }

    @AfterEach
    void clearRegistryAfterTest() {
        clearRegistry();
    }

    private static void clearRegistry() {
        // Tests share one core; start and end with no listeners so a macro count is predictable.
        for (Set<IEventListener> listeners : new ArrayList<>(core().eventRegistry.getListeners().values())) {
            listeners.clear();
        }
    }

    @Test
    void macroTriggersExposeGuiMacrosButNotScriptListeners() throws InterruptedException {
        core().eventRegistry.addEvent(EVENT);
        ScriptTrigger macro = new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, EVENT,
                Path.of("macro.js"), true, false);
        core().eventRegistry.addListener(EVENT, new EventListener(macro, core()));

        EventCustom result = runTestScript("""
                const macros = JsMacros.macroTriggers();
                event.putString("count", String(macros.size()));
                event.putString("event", macros.get(0).getEvent());
                event.putString("enabled", String(macros.get(0).getEnabled()));
                event.putString("scriptListeners", String(JsMacros.listeners("ListenerAccessTestTick").size()));
                """);

        assertEquals("1", result.getString("count"));
        assertEquals(EVENT, result.getString("event"));
        assertEquals("true", result.getString("enabled"));
        assertEquals("0", result.getString("scriptListeners"),
                "macroTriggers must not make profile macros look like script listeners");
    }

    @Test
    void scriptCanDisableAMacroWithoutUnregisteringIt() throws Exception {
        core().eventRegistry.addEvent(EVENT);
        Path file = tempDir.resolve("macro.js");
        Files.writeString(file, "0");
        ScriptTrigger trigger = new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, EVENT, file, true, false);
        EventListener listener = new EventListener(trigger, core());
        core().eventRegistry.addListener(EVENT, listener);

        EventContainer<?> enabledRun = listener.trigger(new EventCustom(core(), EVENT));
        assertNotNull(enabledRun, "an enabled macro starts");
        enabledRun.awaitLock(() -> {});

        EventCustom result = runTestScript("""
                const macros = JsMacros.macroTriggers();
                macros.get(0).setEnabled(false);
                event.putString("enabled", String(macros.get(0).getEnabled()));
                """);

        assertEquals("false", result.getString("enabled"));
        assertFalse(trigger.getEnabled());
        assertEquals(1, core().eventRegistry.getListeners(EVENT).size(),
                "disabling a macro keeps its listener registered");
        assertNull(listener.trigger(new EventCustom(core(), EVENT)),
                "a disabled macro does not start a script");
    }

    @Test
    void disableScriptListenersLeavesMacrosAlone() throws InterruptedException {
        core().eventRegistry.addEvent(EVENT);
        ScriptTrigger macro = new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, EVENT,
                Path.of("macro.js"), true, false);
        core().eventRegistry.addListener(EVENT, new EventListener(macro, core()));

        runTestScript("JsMacros.disableScriptListeners()");

        assertTrue(macro.getEnabled(), "script listener cleanup must not disable profile macros");
        assertEquals(1, core().eventRegistry.getListeners(EVENT).size());
    }

    @Test
    void listenerSnapshotIsDetached() {
        core().eventRegistry.addEvent(EVENT);
        core().eventRegistry.addListener(EVENT, event -> null);
        Map<String, Set<IEventListener>> snapshot = core().eventRegistry.getListenersSnapshot();
        assertEquals(1, snapshot.get(EVENT).size());

        core().eventRegistry.addListener(EVENT, event -> null);
        assertEquals(1, snapshot.get(EVENT).size(), "the snapshot did not follow later additions");
        assertEquals(2, core().eventRegistry.getListeners(EVENT).size());
    }

    /**
     * The no-argument cleanup walks every event name. It used to walk the live map, which can grow
     * while dispatch touches a name it has not seen before.
     */
    @Test
    void noArgCleanupSurvivesConcurrentRegistration() throws Exception {
        core().eventRegistry.addEvent(EVENT);
        AtomicBoolean stop = new AtomicBoolean();
        Thread dispatcher = new Thread(() -> {
            int i = 0;
            while (!stop.get()) {
                core().eventRegistry.getListeners("ListenerAccessTestUnseen-" + (i++ % 500));
            }
        }, "listener-access-test-dispatch");
        dispatcher.start();
        try {
            for (int round = 0; round < 25; round++) {
                for (int i = 0; i < 4; i++) {
                    core().eventRegistry.addListener(EVENT, new ScriptStub(EVENT));
                }
                runTestScript("JsMacros.disableScriptListeners()");
                assertEquals(0, core().eventRegistry.getListeners(EVENT).size(),
                        "round " + round + " left script listeners registered");
            }
        } finally {
            stop.set(true);
            dispatcher.join(5000);
        }
    }

    /** Stands in for the listener {@code JsMacros.on} registers, which removes itself on {@code off()}. */
    private static final class ScriptStub implements FJsMacros.ScriptEventListener {
        private final String event;

        ScriptStub(String event) {
            this.event = event;
        }

        @Override
        public EventContainer<?> trigger(BaseEvent event) {
            return null;
        }

        @Override
        public String getCreatorName() {
            return "listener-access-test";
        }

        @Override
        public MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> getWrapper() {
            return null;
        }

        @Override
        public BaseScriptContext<?> getCtx() {
            return null;
        }

        @Override
        public void off() {
            core().eventRegistry.removeListener(event, this);
        }
    }
}
