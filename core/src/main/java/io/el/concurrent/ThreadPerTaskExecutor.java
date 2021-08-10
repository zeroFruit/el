package io.el.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import static io.el.internal.ObjectUtil.checkNotNull;

public final class ThreadPerTaskExecutor implements Executor {
    private final ThreadFactory threadFactory;

    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        this.threadFactory = checkNotNull(threadFactory, "threadFactory");
    }

    @Override
    public void execute(Runnable task) {
        threadFactory.newThread(task).start();
    }
}
