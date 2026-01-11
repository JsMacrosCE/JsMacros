package com.jsmacrosce.jsmacros.test.stubs;

import org.slf4j.Logger;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.BaseProfile;
import com.jsmacrosce.jsmacros.core.language.BaseWrappedException;

import java.util.concurrent.LinkedBlockingQueue;

public class ProfileStub extends BaseProfile {

    static Thread th;
    static LinkedBlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>();

    static {
        th = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            while (true) {
                try {
                    runnables.take().run();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        th.setDaemon(true);
        th.start();
    }

    public ProfileStub(Core<?, ?> runner, Logger logger) {
        super(runner, logger);
        joinedThreadStack.add(th);
        initRegistries();
    }

    @Override
    public void logError(Throwable ex) {
        BaseWrappedException<?> e = CoreInstanceCreator.core.wrapException(ex);
        LOGGER.error(e.message, ex);
    }

    @Override
    public boolean checkJoinedThreadStack() {
        return joinedThreadStack.contains(Thread.currentThread());
    }
}
