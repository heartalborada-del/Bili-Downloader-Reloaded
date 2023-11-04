package me.heartalborada.biliDownloader.MultiThreadDownload;

import me.heartalborada.biliDownloader.Bili.Beans.LoginData;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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

    public void download(URL url, File filePath, Callback callback) throws IOException {

    }
    public interface Callback {
        void onSuccess(long fileSize);

        void onFailure(Exception e, String cause);

        void onStart(long fileSize);
    }
}
