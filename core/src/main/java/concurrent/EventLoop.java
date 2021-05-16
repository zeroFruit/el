package concurrent;

import java.util.concurrent.TimeUnit;

public interface EventLoop {
    boolean inEventLoop();
    <V> Promise<V> newPromise();
    Promise<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);
}
