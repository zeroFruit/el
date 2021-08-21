package io.el.concurrent;

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
public class DefaultTaskTest {

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
            Task<String> task = new DefaultTask<>(eventLoop);
            task.setSuccess("result");

            TaskListener listener1 = mock(TaskListener.class);
            doNothing().when(listener1).onComplete(any(Task.class));
            task.addListener(listener1);

            TaskListener listener2 = mock(TaskListener.class);
            doNothing().when(listener2).onComplete(any(Task.class));
            task.addListener(listener2);

            verify(listener1, times(1)).onComplete(any(Task.class));
            verify(listener2, times(1)).onComplete(any(Task.class));
        }
    }

    @Nested
    @DisplayName("On await() method")
    class AwaitMethod {

        @Test
        @DisplayName("when timeout negative, then throws exception")
        public void testTimeoutNegative() {
            assertThrows(IllegalArgumentException.class, () -> {
                Task<String> task = new DefaultTask<>(eventLoop);
                task.setSuccess("result");
                task.await(-1, TimeUnit.SECONDS);
            });
        }

        @Test
        @DisplayName("When task completed, then return true")
        public void testDone() throws InterruptedException {
            Task<String> task = new DefaultTask<>(eventLoop);
            task.setSuccess("result");
            assertTrue(task.await(1, TimeUnit.SECONDS).isDone());
        }

        @Test
        @DisplayName("When task done within timeout, then success")
        public void testTaskDoneWithinTimeout() {
            assertTimeout(Duration.ofSeconds(1), () -> {
                CountDownLatch latch = new CountDownLatch(1);
                Task<String> task = new DefaultTask<>(eventLoop);
                Thread t1 = new Thread(() -> {
                    try {
                        assertTrue(task.await(500, TimeUnit.MILLISECONDS).isDone());
                        latch.countDown();
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                Thread t2 = new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        task.setSuccess("result");
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                t1.start();
                t2.start();
                latch.await();
                assertTrue(task.isSuccess());
            });
        }

        @Test
        @DisplayName("When task success within timeout, then success")
        public void testTaskSuccessWithinTimeout() {
            onTimeTestTemplate(new TaskTimeoutTester<String>() {

                @Override
                public void test(Task<String> task) {
                    task.setSuccess("result");
                }

                @Override
                public boolean expected() {
                    return true;
                }
            });
        }

        @Test
        @DisplayName("When task failed within timeout, then failed")
        public void testTaskFailedWithinTimeout() {
            onTimeTestTemplate(new TaskTimeoutTester() {
                @Override
                public void test(Task task) {
                    task.setFailure(new RuntimeException());
                }

                @Override
                public boolean expected() {
                    return false;
                }
            });
        }

        @Test
        @DisplayName("When task cancelled within timeout, then failed")
        public void testTaskCancelledWithinTimeout() {
            onTimeTestTemplate(new TaskTimeoutTester() {
                @Override
                public void test(Task task) throws InterruptedException {
                    task.setFailure(new RuntimeException());
                }

                @Override
                public boolean expected() {
                    return false;
                }
            });
        }

        private void onTimeTestTemplate(TaskTimeoutTester tester) {
            assertTimeout(Duration.ofSeconds(1), () -> {
                CountDownLatch latch = new CountDownLatch(1);
                Task<String> task = new DefaultTask<>(eventLoop);
                Thread t1 = new Thread(() -> {
                    try {
                        task.await();
                    } catch (InterruptedException e) {
                        fail();
                    } finally {
                        latch.countDown();
                    }
                });
                Thread t2 = new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        tester.test(task);
                    } catch (InterruptedException e) {
                        fail();
                    }
                });
                t1.start();
                t2.start();
                latch.await();
                assertEquals(task.isSuccess(), tester.expected());
            });
        }

        @Test
        @DisplayName("When task not done within timeout, then fail")
        public void testTaskTimeout() {
            assertTimeout(Duration.ofSeconds(1), () -> {
                CountDownLatch latch = new CountDownLatch(1);
                Task<String> task = new DefaultTask<>(eventLoop);
                Thread t1 = new Thread(() -> {
                    try {
                        assertFalse(task.await(100, TimeUnit.MILLISECONDS).isDone());
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
                assertFalse(task.isSuccess());
            });
        }

    }

    @Nested
    @DisplayName("On cancel() method")
    class CancelMethod {

        @Test
        @DisplayName("When task already cancelled, then throw exceptions")
        public void testCancelAlready() {
            Task<String> task = new DefaultTask<>(eventLoop);

            assertTrue(task.cancel(false));
            assertThrows(IllegalStateException.class, () -> {
                task.cancel(false);
            });
        }

        @Test
        @DisplayName("When task cancelled, then notify listeners")
        public void testNotifying() throws Exception {
            Task<String> task = new DefaultTask<>(eventLoop);

            TaskListener listener1 = mock(TaskListener.class);
            doNothing().when(listener1).onComplete(any(Task.class));
            task.addListener(listener1);

            assertTrue(task.cancel(false));

            verify(listener1, times(1)).onComplete(any(Task.class));
        }
    }

    @Nested
    @DisplayName("On get() method")
    class GetMethod {

        @Test
        @DisplayName("When task failed, then throw exception")
        public void testFailed() {
            Task<String> task = new DefaultTask<>(eventLoop);

            assertThrows(ExecutionException.class, () -> {
                task.setFailure(new IllegalArgumentException());
                task.get();
            });
        }

        @Test
        @DisplayName("When task cancelled, then throw cancel exception")
        public void testCancelled() {
            Task<String> task = new DefaultTask<>(eventLoop);

            assertThrows(CancellationException.class, () -> {
                task.cancel(false);
                task.get();
            });
        }

        @Test
        @DisplayName("When task success, then return result")
        public void testSuccess() throws ExecutionException, InterruptedException {
            Task<String> task = new DefaultTask<>(eventLoop);

            task.setSuccess("result");
            assertEquals(task.get(), "result");
        }

        @Test
        @DisplayName("When task timeout, then throw timeout exception")
        public void testTimeout() {
            Task<String> task = new DefaultTask<>(eventLoop);

            assertThrows(TimeoutException.class, () -> {
                task.get(100, TimeUnit.MILLISECONDS);
            });
        }
    }

    interface TaskTimeoutTester<V> {
        void test(Task<V> task) throws InterruptedException;
        boolean expected();
    }
}