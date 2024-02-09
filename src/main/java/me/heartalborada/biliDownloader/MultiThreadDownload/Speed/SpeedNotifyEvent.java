package me.heartalborada.biliDownloader.MultiThreadDownload.Speed;

public interface SpeedNotifyEvent {
    void event(long speed, boolean isStop);
}
