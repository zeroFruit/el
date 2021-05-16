package concurrent;

public interface PromiseListener<P extends Promise<?>> {
    void onComplete(P promise) throws Exception;
}
