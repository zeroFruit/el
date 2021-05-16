import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface Promise<V> extends Future<V> {
    boolean isSuccess();
    Promise<V> addListener(PromiseListener<? extends Promise<? super V>> listener);
    Promise<V> removeListener(PromiseListener<? extends Promise<? super V>> listener);
    Promise<V> await(long timeout, TimeUnit unit) throws InterruptedException;
    Promise<V> setSuccess(V result);
    Promise<V> setFailure(Throwable cause);
}