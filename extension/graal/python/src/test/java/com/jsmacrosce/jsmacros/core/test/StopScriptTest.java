package com.jsmacrosce.jsmacros.core.test;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.test.BaseTest;
import com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator;
import com.jsmacrosce.jsmacros.test.stubs.EventRegistryStub;
import com.jsmacrosce.jsmacros.test.stubs.ProfileStub;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StopScriptTest extends BaseTest {
    @Override
    public String getLang() {
        return "py";
    }

    @Test
    void stopsTheCurrentPythonRunAndItsListener() throws InterruptedException {
        Core<ProfileStub, EventRegistryStub> core = CoreInstanceCreator.createCore();
        String name = "StopScriptTestPython";
        core.eventRegistry.addEvent(name);
        core.profile.errors.clear();
        EventCustom event = new EventCustom(core, "test");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventContainer<?> run = core.exec("py", """
                def listener(evt, context):
                    pass
                JsMacros.on('StopScriptTestPython', JavaWrapper.methodToJava(listener))
                event.putString('before', 'yes')
                JsMacros.stopScript()
                event.putString('after', 'unexpected')
                """, null, event, null, failure::set);
        Thread thread = run.getCtx().getMainThread();
        thread.join(10000);
        assertFalse(thread.isAlive());
        assertEquals("yes", event.getString("before"));
        assertNull(event.getString("after"));
        assertTrue(run.getCtx().isContextClosed());
        assertTrue(core.eventRegistry.getListeners(name).isEmpty());
        assertNull(failure.get());
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
    }
}
