package me.heartalborada.biliDownloader.MultiThreadDownload;

import me.heartalborada.biliDownloader.MultiThreadDownload.Speed.DownloadSpeedStat;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashMap;

@SuppressWarnings("unused")
public class MultiThreadDownloader {
    private final int threadCount;
    private final OkHttpClient client;
    public MultiThreadDownloader() {
        this.threadCount = 4;
        client = new OkHttpClient.Builder().build();
    }
    public MultiThreadDownloader(int threadCount) {
        this.threadCount = threadCount;
        client = new OkHttpClient.Builder().build();
    }
    public MultiThreadDownloader(OkHttpClient client) {
        this.threadCount = 4;
        this.client = client.newBuilder().build();
    }
    public MultiThreadDownloader(int threadCount,OkHttpClient client) {
        this.threadCount = threadCount;
        this.client = client.newBuilder().build();
    }

    public DownloadInstance download(URL url, File filePath, Callback callback, LinkedHashMap<String,String> header) throws IOException {
        return new DownloadInstance(
                10 * 1024 * 1024,
                threadCount,
                url,
                callback,
                Path.of(filePath.toURI()),
                new DownloadSpeedStat(callback::newSpeedStat),
                header
        );
    }

    public interface Callback {
        void onSuccess(long fileSize);

        void onFailure(Exception e, String cause);

        void onStart(long fileSize);

        void newSpeedStat(long speed,boolean isStop);
    }
}
