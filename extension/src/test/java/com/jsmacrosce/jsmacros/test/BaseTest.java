package com.jsmacrosce.jsmacros.test;

import org.junit.jupiter.api.BeforeAll;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.EventLockWatchdog;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator;
import com.jsmacrosce.jsmacros.test.stubs.EventRegistryStub;
import com.jsmacrosce.jsmacros.test.stubs.ProfileStub;

/**
 * Runs a script in the shared test core and returns the event it filled in.
 *
 * <p>Extension loading happens once, before the first test. A language runtime is created lazily,
 * so a subclass calls {@link #warmUpLanguage(String)} from its own {@code @BeforeAll}; that runs
 * one no-op script before the measured tests instead of sleeping for a guessed interval.
 */
public abstract class BaseTest {

    private static final Core<ProfileStub, EventRegistryStub> core = CoreInstanceCreator.createCore();

    public abstract String getLang();

    public EventCustom runTestScript(String script) throws InterruptedException {
        return runTestScript(script, 10000);
    }

    public EventCustom runTestScript(String script, int timeout) throws InterruptedException {
        EventCustom event = new EventCustom(core, "test");
        EventContainer<?> ev = core.exec(getLang(), script, null, event, null, null);
        EventLockWatchdog.startWatchdog(ev, null, timeout);
        ev.awaitLock(() -> {});
        return event;
    }

    @BeforeAll
    public static void init() {
        core.extensions.loadExtensions();
    }

    /**
     * Runs a do-nothing script once so later tests do not pay the language runtime's start-up
     * cost. A Graal language initializes its context lazily, and the first context can take
     * longer than a test's own watchdog allows; warming up here keeps a test's timing about the
     * behavior it checks instead of about runtime start-up.
     *
     * @param lang language to warm up, for example {@code "js"} or {@code "py"}
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    protected static void warmUpLanguage(String lang) throws InterruptedException {
        EventCustom event = new EventCustom(core, "warmup");
        EventContainer<?> container = core.exec(lang, "0", null, event, null, null);
        EventLockWatchdog.startWatchdog(container, null, 120_000);
        container.awaitLock(() -> {});
    }

}
