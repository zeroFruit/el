package concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private final EventLoop eventLoop = mock(EventLoop.class);

    @BeforeEach
    public void setup() {
        when(eventLoop.inEventLoop()).thenReturn(true);
    }

    @Nested
    @DisplayName("On addListener() method")
    class AddListenerMethod {

        @Test
        @DisplayName("When after notifying once, then same listener do not notify again")
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

        @Test
        @DisplayName("when timeout negative, then throws exception")
        public void testTimeoutNegative() {
            assertThrows(IllegalArgumentException.class, () -> {
                Promise<String> promise = new DefaultPromise<>(eventLoop);
                promise.setSuccess("result");
                promise.await(-1, TimeUnit.SECONDS);
            });
        }

        @Test
        @DisplayName("When promise completed, then return true")
        public void testDone() throws InterruptedException {
            Promise<String> promise = new DefaultPromise<>(eventLoop);
            promise.setSuccess("result");
            assertTrue(promise.await(1, TimeUnit.SECONDS).isDone());
        }

        @Test
        @DisplayName("When promise done within timeout, then success")
        public void testTaskDoneWithinTimeout() {
            assertTimeout(Duration.ofSeconds(1), () -> {
                CountDownLatch latch = new CountDownLatch(1);
                Promise<String> promise = new DefaultPromise<>(eventLoop);
                Thread t1 = new Thread(() -> {
                    try {
                        assertTrue(promise.await(500, TimeUnit.MILLISECONDS).isDone());
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
        @DisplayName("When promise success within timeout, then success")
        public void testTaskSuccessWithinTimeout() {
            onTimeTestTemplate(new PromiseTimeoutTester<String>() {

                @Override
                public void test(Promise<String> promise) {
                    promise.setSuccess("result");
                }

                @Override
                public boolean expected() {
                    return true;
                }
            });
        }

        @Test
        @DisplayName("When promise failed within timeout, then failed")
        public void testTaskFailedWithinTimeout() {
            onTimeTestTemplate(new PromiseTimeoutTester() {
                @Override
                public void test(Promise promise) {
                    promise.setFailure(new RuntimeException());
                }

                @Override
                public boolean expected() {
                    return false;
                }
            });
        }

        @Test
        @DisplayName("When promise cancelled within timeout, then failed")
        public void testTaskCancelledWithinTimeout() {
            onTimeTestTemplate(new PromiseTimeoutTester() {
                @Override
                public void test(Promise promise) throws InterruptedException {
                    promise.setFailure(new RuntimeException());
                }

                @Override
                public boolean expected() {
                    return false;
                }
            });
        }

        private void onTimeTestTemplate(PromiseTimeoutTester tester) {
            assertTimeout(Duration.ofSeconds(1), () -> {
                CountDownLatch latch = new CountDownLatch(1);
                Promise<String> promise = new DefaultPromise<>(eventLoop);
                Thread t1 = new Thread(() -> {
                    try {
                        promise.await();
                    } catch (InterruptedException e) {
                        fail();
                    } finally {
                        latch.countDown();
                    }
                });
                Thread t2 = new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        tester.test(promise);
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                t1.start();
                t2.start();
                latch.await();
                assertEquals(promise.isSuccess(), tester.expected());
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
                        assertFalse(promise.await(100, TimeUnit.MILLISECONDS).isDone());
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

        @Test
        @DisplayName("When promise already cancelled, then throw exceptions")
        public void testCancelAlready() {
            Promise<String> promise = new DefaultPromise<>(eventLoop);

            assertTrue(promise.cancel(false));
            assertThrows(IllegalStateException.class, () -> {
                promise.cancel(false);
            });
        }

        @Test
        @DisplayName("When promise cancelled, then notify listeners")
        public void testNotifying() throws Exception {
            Promise<String> promise = new DefaultPromise<>(eventLoop);

            PromiseListener listener1 = mock(PromiseListener.class);
            doNothing().when(listener1).onComplete(any(Promise.class));
            promise.addListener(listener1);

            assertTrue(promise.cancel(false));

            verify(listener1, times(1)).onComplete(any(Promise.class));
        }
    }

    @Nested
    @DisplayName("On get() method")
    class GetMethod {

        @Test
        @DisplayName("When promise failed, then throw exception")
        public void testFailed() {
            Promise<String> promise = new DefaultPromise<>(eventLoop);

            assertThrows(ExecutionException.class, () -> {
                promise.setFailure(new IllegalArgumentException());
                promise.get();
            });
        }

        @Test
        @DisplayName("When promise cancelled, then throw cancel exception")
        public void testCancelled() {
            Promise<String> promise = new DefaultPromise<>(eventLoop);

            assertThrows(CancellationException.class, () -> {
                promise.cancel(false);
                promise.get();
            });
        }

        @Test
        @DisplayName("When promise success, then return result")
        public void testSuccess() throws ExecutionException, InterruptedException {
            Promise<String> promise = new DefaultPromise<>(eventLoop);

            promise.setSuccess("result");
            assertEquals(promise.get(), "result");
        }

        @Test
        @DisplayName("When promise timeout, then throw timeout exception")
        public void testTimeout() {
            Promise<String> promise = new DefaultPromise<>(eventLoop);

            assertThrows(TimeoutException.class, () -> {
                promise.get(100, TimeUnit.MILLISECONDS);
            });
        }
    }

    interface PromiseTimeoutTester<V> {
        void test(Promise<V> promise) throws InterruptedException;
        boolean expected();
    }
}
