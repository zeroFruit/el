package io.el.concurrent;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultEventLoopChooserFactory implements EventLoopChooserFactory {
    public static final DefaultEventLoopChooserFactory INSTANCE = new DefaultEventLoopChooserFactory();

    private DefaultEventLoopChooserFactory() {}

    @Override
    public EventLoopChooser newChooser(List<EventLoop> loops) {
        return new GenericEventLoopChooser(loops);
    }

    private static final class GenericEventLoopChooser implements EventLoopChooser {

        private final AtomicLong idx = new AtomicLong();
        private final List<EventLoop> eventLoops;

        GenericEventLoopChooser(List<EventLoop> eventLoops) {
            this.eventLoops = eventLoops;
        }

        @Override
        public EventLoop next() {
            return eventLoops.get((int) Math.abs(idx.getAndIncrement()) % eventLoops.size());
        }
    }
}
