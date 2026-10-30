package com.jsmacrosce.jsmacros.core.test;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.classes.Registrable;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.IEventListener;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.core.library.impl.FJsMacros;
import com.jsmacrosce.jsmacros.core.service.EventService;
import com.jsmacrosce.jsmacros.core.service.ServiceTrigger;
import com.jsmacrosce.jsmacros.test.BaseTest;
import com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator;
import com.jsmacrosce.jsmacros.test.stubs.EventRegistryStub;
import com.jsmacrosce.jsmacros.test.stubs.ProfileStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StopScriptTest extends BaseTest {
    private final Core<ProfileStub, EventRegistryStub> core = CoreInstanceCreator.createCore();

    private static final class TestRegistrable implements Registrable<Object> {
        private final AtomicInteger removals = new AtomicInteger();

        @Override
        public Object register() {
            return this;
        }

        @Override
        public Object unregister() {
            removals.incrementAndGet();
            return this;
        }
    }

    @Override
    public String getLang() {
        return "js";
    }

    private void awaitThread(Thread thread) throws InterruptedException {
        assertNotNull(thread);
        thread.join(10000);
        assertFalse(thread.isAlive(), "script did not finish");
    }

    @Test
    void stopsOnlyTheCurrentRunAndItsListeners() throws InterruptedException {
        String name = "StopScriptTestTopLevel";
        core.eventRegistry.addEvent(name);
        core.profile.errors.clear();

        EventCustom otherEvent = new EventCustom(core, "other");
        EventContainer<?> other = core.exec("js", """
                JsMacros.on('StopScriptTestTopLevel', JavaWrapper.methodToJava(() => {}));
                """, null, otherEvent, null, null);
        awaitThread(other.getCtx().getMainThread());

        EventCustom event = new EventCustom(core, "test");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventContainer<?> run = core.exec("js", """
                JsMacros.on('StopScriptTestTopLevel', JavaWrapper.methodToJava(() => {}));
                JsMacros.once('StopScriptTestTopLevel', JavaWrapper.methodToJava(() => {}));
                event.putString('before', 'yes');
                JsMacros.stopScript();
                event.putString('after', 'unexpected');
                """, null, event, null, failure::set);
        awaitThread(run.getCtx().getMainThread());

        assertEquals("yes", event.getString("before"));
        assertNull(event.getString("after"));
        assertTrue(run.getCtx().isContextClosed());
        assertFalse(run.isLocked());
        assertNull(failure.get());
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
        assertEquals(1, core.eventRegistry.getListeners(name).size());
        assertFalse(other.getCtx().isContextClosed());

        core.eventRegistry.getListeners(name).forEach(IEventListener::off);
        other.getCtx().closeContext();
    }

    @Test
    void stopFromListenerRemovesItsOtherListenersWithoutLogging() throws InterruptedException {
        String trigger = "StopScriptTestCallback";
        String other = "StopScriptTestOtherCallback";
        core.eventRegistry.addEvent(trigger);
        core.eventRegistry.addEvent(other);
        core.profile.errors.clear();

        EventCustom event = new EventCustom(core, "test");
        EventContainer<?> run = core.exec("js", """
                JsMacros.on('StopScriptTestCallback', JavaWrapper.methodToJava(() => {
                  event.putString('before', 'yes');
                  JsMacros.stopScript();
                  event.putString('after', 'unexpected');
                }));
                JsMacros.once('StopScriptTestOtherCallback', JavaWrapper.methodToJava(() => {}));
                """, null, event, null, null);
        awaitThread(run.getCtx().getMainThread());

        IEventListener listener = core.eventRegistry.getListeners(trigger).iterator().next();
        EventCustom cancellable = new EventCustom(core, trigger);
        cancellable.cancelable = true;
        EventContainer<?> callback = listener.trigger(cancellable);
        awaitThread(callback.getLockThread());

        assertEquals("yes", event.getString("before"));
        assertNull(event.getString("after"));
        assertTrue(run.getCtx().isContextClosed());
        assertFalse(callback.isLocked());
        assertTrue(core.eventRegistry.getListeners(trigger).isEmpty());
        assertTrue(core.eventRegistry.getListeners(other).isEmpty());
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
    }

    @Test
    void stopInterruptsAnEventWaitAndRemovesItsListener() throws InterruptedException {
        String name = "StopScriptTestWaiting";
        core.eventRegistry.addEvent(name);
        core.profile.errors.clear();

        EventCustom event = new EventCustom(core, "test");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventContainer<?> run = core.exec("js", """
                JavaWrapper.methodToJavaAsync(() => {
                  JsMacros.waitForEvent('StopScriptTestWaiting');
                  event.putString('afterWait', 'unexpected');
                }).run();
                while (JsMacros.listeners('StopScriptTestWaiting').isEmpty()) {
                  JavaWrapper.deferCurrentTask();
                }
                JsMacros.stopScript();
                """, null, event, null, failure::set);
        awaitThread(run.getCtx().getMainThread());
        for (Thread thread : run.getCtx().getBoundThreads()) awaitThread(thread);

        assertTrue(run.getCtx().isContextClosed());
        assertTrue(core.eventRegistry.getListeners(name).isEmpty());
        assertNull(event.getString("afterWait"));
        assertNull(failure.get());
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
    }

    @Test
    void stopCancelsCallbacksAlreadyQueuedInTheSameRun() throws InterruptedException {
        String first = "StopScriptTestActiveCallback";
        String second = "StopScriptTestQueuedCallback";
        core.eventRegistry.addEvent(first);
        core.eventRegistry.addEvent(second);
        core.profile.errors.clear();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        EventCustom event = new EventCustom(core, "test");
        event.putObject("entered", entered);
        event.putObject("proceed", proceed);
        EventContainer<?> run = core.exec("js", """
                JsMacros.on('StopScriptTestActiveCallback', JavaWrapper.methodToJava(() => {
                  event.getObject('entered').countDown();
                  event.getObject('proceed').await();
                  JsMacros.stopScript();
                }));
                JsMacros.on('StopScriptTestQueuedCallback', JavaWrapper.methodToJava(() => {
                  event.putString('queued', 'unexpected');
                }));
                """, null, event, null, null);
        awaitThread(run.getCtx().getMainThread());
        EventContainer<?> active = core.eventRegistry.getListeners(first).iterator().next()
                .trigger(new EventCustom(core, first));
        assertTrue(entered.await(10, TimeUnit.SECONDS));
        EventContainer<?> queued = core.eventRegistry.getListeners(second).iterator().next()
                .trigger(new EventCustom(core, second));
        try {
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (queued.getLockThread().getState() != Thread.State.WAITING && System.nanoTime() < deadline) Thread.sleep(10);
            assertEquals(Thread.State.WAITING, queued.getLockThread().getState());
        } finally {
            proceed.countDown();
        }
        awaitThread(active.getLockThread());
        awaitThread(queued.getLockThread());
        assertNull(event.getString("queued"));
        assertTrue(core.eventRegistry.getListeners(first).isEmpty());
        assertTrue(core.eventRegistry.getListeners(second).isEmpty());
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
    }

    @Test
    void stopUnblocksAnEventAlreadyBeingDispatchedToAWaiter() throws InterruptedException {
        String name = "StopScriptTestDispatchingWait";
        core.eventRegistry.addEvent(name);
        core.profile.errors.clear();
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        EventCustom event = new EventCustom(core, "test");
        event.putObject("ready", ready);
        event.putObject("proceed", proceed);
        EventContainer<?> run = core.exec("js", """
                JavaWrapper.methodToJavaAsync(() => JsMacros.waitForEvent('StopScriptTestDispatchingWait',
                  JavaWrapper.methodToJava(() => {
                    event.getObject('ready').countDown();
                    event.getObject('proceed').await();
                    return true;
                  }))).run();
                while (JsMacros.listeners('StopScriptTestDispatchingWait').isEmpty()) JavaWrapper.deferCurrentTask();
                """, null, event, null, null);
        awaitThread(run.getCtx().getMainThread());
        IEventListener listener = core.eventRegistry.getListeners(name).iterator().next();
        Thread dispatch = new Thread(() -> listener.trigger(new EventCustom(core, name)));
        dispatch.setDaemon(true);
        dispatch.start();
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        Thread stop = new Thread(() -> new FJsMacros(run.getCtx()).stopScript());
        stop.setDaemon(true);
        stop.start();
        try {
            awaitThread(dispatch);
            awaitThread(stop);
        } finally {
            proceed.countDown();
        }
        assertTrue(core.eventRegistry.getListeners(name).isEmpty());
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
    }

    @Test
    void unexpectedScriptErrorsStillReachTheFailureHandler() throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventContainer<?> run = core.exec("js", "throw new Error('real failure');", null,
                new EventCustom(core, "test"), null, failure::set);
        awaitThread(run.getCtx().getMainThread());
        assertNotNull(failure.get());
        assertTrue(failure.get().getMessage().contains("real failure"));
    }

    @Test
    void stoppingAChildScriptCallsItsCompletionCallbackWithoutAnError() throws InterruptedException {
        core.profile.errors.clear();
        CountDownLatch completed = new CountDownLatch(1);
        EventCustom event = new EventCustom(core, "test");
        event.putObject("completed", completed);
        EventContainer<?> parent = core.exec("js", """
                JsMacros.runScript('js', 'JsMacros.stopScript();', JavaWrapper.methodToJava((error) => {
                  event.putString('result', error == null ? 'success' : String(error));
                  event.getObject('completed').countDown();
                }));
                """, null, event, null, null);
        awaitThread(parent.getCtx().getMainThread());
        assertTrue(completed.await(10, TimeUnit.SECONDS));
        assertEquals("success", event.getString("result"));
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
        parent.getCtx().closeContext();
    }

    @Test
    void stoppingAFileScriptCallsItsCompletionCallback(@TempDir Path temp) throws Exception {
        Path script = temp.resolve("stop.js");
        Files.writeString(script, "JsMacros.stopScript();");
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        EventContainer<?> run = core.exec(new ScriptTrigger(ScriptTrigger.TriggerType.EVENT,
                "StopScriptTest", script, true, false), new EventCustom(core, "test"),
                completed::countDown, failure::set);
        awaitThread(run.getCtx().getMainThread());
        assertTrue(completed.await(10, TimeUnit.SECONDS));
        assertNull(failure.get());
    }

    @Test
    void stopFromAnAsyncWrapperDoesNotLogCancellation() throws InterruptedException {
        core.profile.errors.clear();
        EventCustom event = new EventCustom(core, "test");
        EventContainer<?> run = core.exec("js", """
                JavaWrapper.methodToJavaAsync(() => {
                  event.putObject('worker', Java.type('java.lang.Thread').currentThread());
                  JsMacros.stopScript();
                  event.putString('after', 'unexpected');
                }).run();
                """, null, event, null, null);
        awaitThread(run.getCtx().getMainThread());
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (event.getObject("worker") == null && System.nanoTime() < deadline) Thread.sleep(10);
        Thread worker = (Thread) event.getObject("worker");
        awaitThread(worker);
        assertTrue(run.getCtx().isContextClosed());
        assertNull(event.getString("after"));
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
    }

    @Test
    void stopServiceRunsItsStopCallbacks(@TempDir Path temp) throws Exception {
        String name = "StopScriptTestService";
        core.profile.errors.clear();
        Path script = temp.resolve("service.js");
        Files.writeString(script, """
                event.stopListener = JavaWrapper.methodToJava(() => event.putString('stop', 'yes'));
                event.postStopListener = JavaWrapper.methodToJava(() => event.putString('post', 'yes'));
                while (event.getObject('resource') === null) Time.sleep(10);
                event.unregisterOnStop(false, event.getObject('resource'));
                event.putString('ready', 'yes');
                while (event.getString('go') !== 'yes') Time.sleep(10);
                JsMacros.stopScript();
                event.putString('after', 'unexpected');
                """);
        assertTrue(core.services.registerService(name, new ServiceTrigger(script, false)));
        core.services.startService(name);
        EventService event = core.services.getServiceData(name);
        TestRegistrable resource = new TestRegistrable();
        event.putObject("resource", resource);
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (event.getString("ready") == null && System.nanoTime() < deadline) Thread.sleep(10);
        assertEquals("yes", event.getString("ready"));
        EventContainer<?> run = core.getContexts().stream()
                .filter(ctx -> ctx.getTriggeringEvent() == event)
                .findFirst().map(EventContainer::new).orElse(null);
        assertNotNull(run);
        event.putString("go", "yes");
        awaitThread(run.getCtx().getMainThread());

        assertEquals("yes", event.getString("stop"));
        assertEquals("yes", event.getString("post"));
        assertEquals(1, resource.removals.get());
        assertNull(event.getString("after"));
        assertTrue(run.getCtx().isContextClosed());
        assertFalse(core.services.isCrashed(name));
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
        core.services.unregisterService(name);
    }

    @Test
    void serviceCanStopItselfWhileAnotherThreadStopsIt(@TempDir Path temp) throws Exception {
        String name = "StopScriptTestConcurrentService";
        String trigger = "StopScriptTestServiceTrigger";
        core.eventRegistry.addEvent(trigger);
        core.profile.errors.clear();
        Path script = temp.resolve("service.js");
        Files.writeString(script, """
                event.stopListener = JavaWrapper.methodToJava(() => event.putString('stop', 'yes'));
                JsMacros.on('StopScriptTestServiceTrigger', JavaWrapper.methodToJava(() => {
                  event.getObject('entered').countDown();
                  event.getObject('proceed').await();
                  JsMacros.stopScript();
                  event.putString('after', 'unexpected');
                }));
                event.putString('ready', 'yes');
                """);
        assertTrue(core.services.registerService(name, new ServiceTrigger(script, false)));
        core.services.startService(name);
        EventService event = core.services.getServiceData(name);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        event.putObject("entered", entered);
        event.putObject("proceed", proceed);
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (event.getString("ready") == null && System.nanoTime() < deadline) Thread.sleep(10);
        assertEquals("yes", event.getString("ready"));
        Thread main = core.getContexts().stream().filter(ctx -> ctx.getTriggeringEvent() == event)
                .findFirst().orElseThrow().getMainThread();
        awaitThread(main);
        IEventListener listener = core.eventRegistry.getListeners(trigger).iterator().next();
        EventContainer<?> callback = listener.trigger(new EventCustom(core, trigger));
        assertTrue(entered.await(10, TimeUnit.SECONDS));

        Thread externalStop = new Thread(() -> core.services.stopService(name));
        externalStop.setDaemon(true);
        externalStop.start();
        try {
            deadline = System.nanoTime() + 10_000_000_000L;
            while (externalStop.getState() != Thread.State.WAITING && System.nanoTime() < deadline) Thread.sleep(10);
            assertEquals(Thread.State.WAITING, externalStop.getState(), "service stop hook did not wait for the script");
        } finally {
            proceed.countDown();
        }
        awaitThread(callback.getLockThread());
        awaitThread(externalStop);

        assertEquals("yes", event.getString("stop"));
        assertNull(event.getString("after"));
        assertTrue(core.eventRegistry.getListeners(trigger).isEmpty());
        assertFalse(core.services.isCrashed(name));
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
        core.services.unregisterService(name);
    }

    @Test
    void stopRequestedFromAServiceStopHookDoesNotReenterShutdown(@TempDir Path temp) throws Exception {
        String name = "StopScriptTestStopHook";
        core.profile.errors.clear();
        Path script = temp.resolve("service.js");
        Files.writeString(script, """
                event.stopListener = JavaWrapper.methodToJava(() => {
                  JsMacros.stopScript();
                  event.putString('stop', 'yes');
                });
                event.putString('ready', 'yes');
                """);
        assertTrue(core.services.registerService(name, new ServiceTrigger(script, false)));
        core.services.startService(name);
        EventService event = core.services.getServiceData(name);
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (event.getString("ready") == null && System.nanoTime() < deadline) Thread.sleep(10);
        assertEquals("yes", event.getString("ready"));

        Thread stop = new Thread(() -> core.services.stopService(name));
        stop.setDaemon(true);
        stop.start();
        awaitThread(stop);
        assertEquals("yes", event.getString("stop"));
        assertFalse(core.services.isRunning(name));
        assertTrue(core.profile.errors.isEmpty(), core.profile.errors.toString());
        core.services.unregisterService(name);
    }
}
