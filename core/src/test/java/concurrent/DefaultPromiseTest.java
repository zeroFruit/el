package concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class DefaultPromiseTest {

    @Nested
    @DisplayName("On addListener() method")
    class AddListenerMethod {
        final EventLoop eventLoop = mock(EventLoop.class);

        @BeforeEach
        public void setup() {
            when(eventLoop.inEventLoop()).thenReturn(true);
        }

        @Test
        @DisplayName("When after notifying once, same listener do not notify again")
        public void testNoDoubleNotifying() throws Exception {
            Promise<String> promise = new DefaultPromise<>(eventLoop);
            promise.setSuccess("result");

            PromiseListener listener1 = mock(PromiseListener.class);
            doNothing().when(listener1).onComplete(any(Promise.class));
            promise.addListener(listener1);

            PromiseListener listener2 = mock(PromiseListener.class);
            doNothing().when(listener2).onComplete(any(Promise.class));
            promise.addListener(listener2);

            verify(listener1, times(1)).onComplete(any(Promise.class));
            verify(listener2, times(1)).onComplete(any(Promise.class));
        }
    }

    @Nested
    @DisplayName("On await() method")
    class AwaitMethod {
        final EventLoop eventLoop = mock(EventLoop.class);

        @Test
        @DisplayName("when timeout negative, throws exception")
        public void testTimeoutNegative() {
            assertThrows(IllegalArgumentException.class, () -> {
                Promise<String> promise = new DefaultPromise<>(eventLoop);
                promise.setSuccess("result");
                promise.await(-1, TimeUnit.SECONDS);
            });
        }

        @Test
        @DisplayName("When promise completed, return true")
        public void testDone() throws InterruptedException {
            Promise<String> promise = new DefaultPromise<>(eventLoop);
            promise.setSuccess("result");
            assertTrue(promise.await(1, TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("When promise done within timeout, then success")
        public void testTaskDoneWithinTimeout() {
            assertTimeout(Duration.ofSeconds(1), () -> {
                CountDownLatch latch = new CountDownLatch(1);
                Promise<String> promise = new DefaultPromise<>(eventLoop);
                Thread t1 = new Thread(() -> {
                    try {
                        assertTrue(promise.await(500, TimeUnit.MILLISECONDS));
                        latch.countDown();
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                Thread t2 = new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        promise.setSuccess("result");
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                t1.start();
                t2.start();
                latch.await();
                assertTrue(promise.isSuccess());
            });
        }

        @Test
        @DisplayName("When promise not done within timeout, then fail")
        public void testTaskTimeout() {
            assertTimeout(Duration.ofSeconds(1), () -> {
                CountDownLatch latch = new CountDownLatch(1);
                Promise<String> promise = new DefaultPromise<>(eventLoop);
                Thread t1 = new Thread(() -> {
                    try {
                        assertFalse(promise.await(100, TimeUnit.MILLISECONDS));
                        latch.countDown();
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                Thread t2 = new Thread(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                t1.start();
                t2.start();
                latch.await();
                assertFalse(promise.isSuccess());
            });
        }
    }

    @Nested
    @DisplayName("On cancel() method")
    class CancelMethod {
        final EventLoop eventLoop = mock(EventLoop.class);

        @BeforeEach
        public void setup() {
            when(eventLoop.inEventLoop()).thenReturn(true);
        }

        @Test
        @DisplayName("When promise already cancelled, throw exceptions")
        public void testCancelAlready() {
            Promise<String> promise = new DefaultPromise<>(eventLoop);

            assertTrue(promise.cancel(false));
            assertThrows(IllegalStateException.class, () -> {
                promise.cancel(false);
            });
        }

        @Test
        @DisplayName("When promise cancelled, notify listeners")
        public void testNotifying() throws Exception {
            Promise<String> promise = new DefaultPromise<>(eventLoop);

            PromiseListener listener1 = mock(PromiseListener.class);
            doNothing().when(listener1).onComplete(any(Promise.class));
            promise.addListener(listener1);

            assertTrue(promise.cancel(false));

            verify(listener1, times(1)).onComplete(any(Promise.class));
        }

    }
}
