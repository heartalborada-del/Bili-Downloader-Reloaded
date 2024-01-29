package me.heartalborada.biliDownloader.Interfaces;

import java.util.Map;

public interface Selection<T> {
    void bind(T obj, SelectionCallback<T> cb);
    void binds(Map<T,SelectionCallback<T>> map);
    void rerender();
    void close();
    void begin();
}
