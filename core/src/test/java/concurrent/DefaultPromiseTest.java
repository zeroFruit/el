package concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class DefaultPromiseTest {

    @Nested
    @DisplayName("On addListener() method")
    class OnAddListenerMethod {
        EventLoop eventLoop = mock(EventLoop.class);

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
}
