package com.jsmacrosce.jsmacros.core.language;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.BaseProfile;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEventRegistry;
import com.jsmacrosce.jsmacros.core.threads.JsMacrosThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Checks that a closing script context interrupts the threads the script owns but not the engine
 * threads that call into the script for a single callback. Also checks that language teardown runs
 * once and cannot keep the context in the runner's open-context list.
 */
public final class ContextCloseTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Core<BaseProfile, BaseEventRegistry> core = core();

        borrowedCallbackThreadIsNotInterrupted(core);
        borrowedCallbackThreadSurvivesCloseFromAnotherThread(core);
        ownedThreadIsInterrupted(core);
        poolCallbackThreadStaysOwned(core);
        explicitlyOwnedThreadKeepsOwnership(core);
        teardownRunsOnce(core);
        teardownFailureStillUnregisters(core);

        System.out.println("ContextClose: " + checks + " checks passed");
    }

    private static void borrowedCallbackThreadIsNotInterrupted(Core<?, ?> core) {
        TestContext ctx = new TestContext(core, false);
        Thread.interrupted();

        ctx.bindCallerThread();
        check(ctx.getBoundThreads().contains(Thread.currentThread()), "the callback thread is bound");
        check(ctx.borrowedThreads.contains(Thread.currentThread()), "a non-pool callback thread is borrowed");

        ctx.closeContext();
        check(!Thread.currentThread().isInterrupted(), "closing leaves a borrowed engine thread uninterrupted");
        Thread.interrupted();
    }

    private static void borrowedCallbackThreadSurvivesCloseFromAnotherThread(Core<?, ?> core) throws Exception {
        TestContext ctx = new TestContext(core, false);
        CountDownLatch bound = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        Thread engine = new Thread(() -> {
            ctx.bindCallerThread();
            bound.countDown();
            try {
                release.await();
            } catch (InterruptedException expected) {
                interrupted.countDown();
            }
        }, "borrowed callback thread");
        engine.start();
        check(bound.await(5, TimeUnit.SECONDS), "the borrowed callback thread binds");

        ctx.closeContext();
        check(!interrupted.await(200, TimeUnit.MILLISECONDS),
            "closing from another thread leaves a borrowed callback thread uninterrupted");

        release.countDown();
        engine.join(TimeUnit.SECONDS.toMillis(5));
        check(!engine.isAlive(), "the borrowed callback thread exits when released");
    }

    private static void ownedThreadIsInterrupted(Core<?, ?> core) throws Exception {
        TestContext ctx = new TestContext(core, false);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        Thread owned = new Thread(() -> {
            started.countDown();
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException expected) {
                interrupted.countDown();
            }
        }, "owned script thread");
        owned.start();
        check(started.await(5, TimeUnit.SECONDS), "the owned thread starts");

        ctx.bindThread(owned);
        ctx.closeContext();

        check(interrupted.await(5, TimeUnit.SECONDS), "closing interrupts a script-owned thread");
        owned.join(TimeUnit.SECONDS.toMillis(5));
        check(!owned.isAlive(), "the interrupted owned thread exits");
    }

    private static void poolCallbackThreadStaysOwned(Core<?, ?> core) throws Exception {
        TestContext ctx = new TestContext(core, false);
        JsMacrosThreadPool.PoolThread pool = new JsMacrosThreadPool.PoolThread();
        CountDownLatch bound = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        pool.runTask(() -> {
            ctx.bindCallerThread();
            bound.countDown();
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException expected) {
                interrupted.countDown();
            }
        });
        pool.start();
        check(bound.await(5, TimeUnit.SECONDS), "the pool callback runs");

        check(ctx.getBoundThreads().contains(pool), "the pool callback thread is bound");
        check(!ctx.borrowedThreads.contains(pool), "a pool callback thread stays owned and interruptible");

        ctx.closeContext();
        check(interrupted.await(5, TimeUnit.SECONDS), "closing interrupts a callback running on a pool thread");
        pool.join(TimeUnit.SECONDS.toMillis(5));
        check(!pool.isAlive(), "the interrupted pool callback thread exits");
    }

    private static void explicitlyOwnedThreadKeepsOwnership(Core<?, ?> core) throws Exception {
        TestContext ctx = new TestContext(core, false);
        CountDownLatch ran = new CountDownLatch(1);
        Thread owned = new Thread(() -> {
            ctx.bindThread(Thread.currentThread());
            ctx.bindCallerThread();
            ran.countDown();
        }, "explicitly owned thread");
        owned.start();
        check(ran.await(5, TimeUnit.SECONDS), "the explicitly owned thread runs");
        owned.join(TimeUnit.SECONDS.toMillis(5));

        check(!owned.isAlive(), "the explicitly owned thread exits");
        check(!ctx.borrowedThreads.contains(owned), "binding a callback does not demote an explicitly owned thread");
        ctx.closeContext();
    }

    private static void teardownRunsOnce(Core<?, ?> core) {
        TestContext ctx = new TestContext(core, false);
        ctx.closeContext();
        ctx.closeContext();
        ctx.closeContext(true);
        check(ctx.teardownCalls.get() == 1, "language teardown runs once even when close is repeated");
    }

    private static void teardownFailureStillUnregisters(Core<?, ?> core) {
        TestContext ctx = new TestContext(core, true);
        core.getContexts().add(ctx);
        expectThrows(IllegalStateException.class, ctx::closeContext, "a teardown failure propagates");
        check(!core.getContexts().contains(ctx), "a failed teardown still unregisters the context");
    }

    private static Core<BaseProfile, BaseEventRegistry> core() throws Exception {
        File configFolder = Files.createTempDirectory("jsmacros-context-close-config").toFile();
        File macroFolder = Files.createTempDirectory("jsmacros-context-close-macros").toFile();
        Logger logger = LoggerFactory.getLogger("ContextCloseTest");
        return new Core<>(
            runner -> new BaseEventRegistry(runner) {
                @Override
                public void addScriptTrigger(ScriptTrigger rawmacro) {
                }

                @Override
                public boolean removeScriptTrigger(ScriptTrigger rawmacro) {
                    return false;
                }

                @Override
                public List<ScriptTrigger> getScriptTriggers() {
                    return Collections.emptyList();
                }
            },
            (runner, log) -> new BaseProfile(runner, log) {
                @Override
                public void logError(Throwable ex) {
                    ex.printStackTrace();
                }

                @Override
                public boolean checkJoinedThreadStack() {
                    return false;
                }
            },
            configFolder,
            macroFolder,
            logger
        );
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String description) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                checks++;
                return;
            }
            throw new AssertionError(description + ": expected " + expected.getSimpleName() + " but got " + failure, failure);
        }
        throw new AssertionError(description + ": expected " + expected.getSimpleName() + " but nothing was thrown");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    private static final class TestContext extends BaseScriptContext<Object> {
        private final AtomicInteger teardownCalls = new AtomicInteger();
        private final boolean failTeardown;

        private TestContext(Core<?, ?> runner, boolean failTeardown) {
            super(runner, null, null);
            this.failTeardown = failTeardown;
        }

        @Override
        public boolean isMultiThreaded() {
            return false;
        }

        @Override
        protected void doSubclassClose() {
            teardownCalls.incrementAndGet();
            if (failTeardown) {
                throw new IllegalStateException("language teardown failed");
            }
        }
    }
}
