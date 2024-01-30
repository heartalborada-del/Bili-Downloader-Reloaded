package me.heartalborada.biliDownloader.MultiThreadDownload.Speed;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadSpeedStat {
    private final SpeedNotifyEvent speedNotifyEvent;
    private final AtomicLong counter = new AtomicLong(0);
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
    private long preCount = 0;

    private final boolean isShutdown = false;
    public DownloadSpeedStat(SpeedNotifyEvent speedNotifyEvent) {
        this.speedNotifyEvent = speedNotifyEvent;
    }

    public void add(long val) {
        counter.addAndGet(val);
    }

    public void start() {
        if (!scheduledThreadPoolExecutor.isShutdown()) {
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
                long nowCount = counter.get();
                speedNotifyEvent.event(nowCount - preCount, isShutdown);
                preCount = nowCount;
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        scheduledThreadPoolExecutor.shutdown();
    }
}
