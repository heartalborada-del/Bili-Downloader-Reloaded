package me.heartalborada.biliDownloader.Interfaces;

public interface ProcessProgress {
    void updateText(String string);
    void updateText(CharSequence charSequence);
    void setTotalSize(long size);
    void update(long processed);
    void update(long processed,long size);
    void setFailed();
    void rerender();
    void close();}
