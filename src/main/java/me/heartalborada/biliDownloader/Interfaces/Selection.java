package me.heartalborada.biliDownloader.Interfaces;

import java.util.Map;

public interface Selection<T> extends AutoCloseable {
    void bind(T obj, SelectionCallback<T> cb);
    void binds(Map<T,SelectionCallback<T>> map);
    void rerender();
    void start();
    boolean cancel();
    boolean isCancelled();
    boolean isDone();
}
