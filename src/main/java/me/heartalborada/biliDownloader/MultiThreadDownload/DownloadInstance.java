package me.heartalborada.biliDownloader.MultiThreadDownload;

import lombok.Getter;
import me.heartalborada.biliDownloader.Bili.Exceptions.BadRequestDataException;
import me.heartalborada.biliDownloader.MultiThreadDownload.Speed.DownloadSpeedStat;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.*;

public class DownloadInstance {
    private final URL url;
    private final ExecutorService service;
    private final BlockingQueue<BufferData> dataQueue = new PriorityBlockingQueue<>();
    private final long fileSize;
    private final MultiThreadDownloader.Callback callback;
    private final Path savePath;
    private final DownloadSpeedStat stat;
    private final OkHttpClient client;
    private long THREAD_COUNTER = 0;
    @Getter
    private volatile boolean isDone = false, isFailed = false;

    public DownloadInstance(
            long threshold,
            int threadCount,
            URL url,
            MultiThreadDownloader.Callback callback,
            Path savePath,
            DownloadSpeedStat stat,
            OkHttpClient client
    ) throws IOException {
        this.stat = stat;
        service = Executors.newFixedThreadPool(threadCount + 1);
        this.url = url;
        this.callback = callback;
        this.savePath = savePath;
        this.client = client.newBuilder().connectionPool(new ConnectionPool(threadCount+1, 120, TimeUnit.SECONDS)).build();
        long length = -1;
        boolean isAllowMultiThread = false;
        try (Response resp = this.client.newCall(new Request.Builder().url(url).build()).execute()) {
            if (!Objects.equals(resp.header("Transfer-Encoding"), "chunked") && (Objects.equals(resp.header("Accept-Ranges"), "bytes") || resp.header("Content-Length") != null))
                isAllowMultiThread = true;
            if (!Objects.equals(resp.header("Content-Length"), null))
                length = Long.parseLong(resp.header("Content-Length"));
        }
        this.fileSize = length;
        if (fileSize == -1)
            throw new RuntimeException("Cannot get download file size.");
        File f = new File(savePath.toUri());
        if (!f.getParentFile().exists() && !f.getParentFile().mkdirs())
            throw new IOException(String.format("Cannot make dictionaries: %s.", f.getPath()));
        if (f.exists() && !f.delete())
            throw new IOException(String.format("Cannot delete: %s.", f.getPath()));
        callback.onStart(fileSize);
        service.submit(new SaveTask());
        if (isAllowMultiThread && fileSize > threshold) {
            long startPos = 0, endPos = 0;
            long count = fileSize / threshold;
            for (long i = 0; i < count; i++) {
                startPos = i * threshold;
                endPos = startPos + threshold - 1;
                service.submit(new Task(startPos, endPos));
            }
            if (endPos < fileSize - 1) {
                service.submit(new Task(endPos + 1, fileSize - 1));
            }
        } else {
            service.submit(new Task(0, fileSize));
        }
        stat.start();
    }

    public boolean stop() {
        if (!isDone) isFailed = true;
        stat.stop();
        service.shutdownNow();
        return service.isShutdown();
    }

    class Task implements Runnable {
        private final long startPos;
        private final long endPos;
        private final long serialNum;

        public Task(long startPos, long endPos) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.serialNum = THREAD_COUNTER++;
        }

        //@SuppressWarnings("all")
        @Override
        public void run() {
            Request req = new Request.Builder().url(url).header("Range", String.format("bytes=%d-%d", startPos, endPos)).build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.body() == null)
                    throw new BadRequestDataException(resp.code(), "The url has no data");
                BufferData buffData = new BufferData(serialNum, startPos, endPos);
                int len;
                InputStream inputStream = resp.body().byteStream();
                byte[] data = new byte[8 * 1024];
                while ((len = inputStream.read(data)) > 0) {
                    stat.add(len);
                    buffData.write(data, 0, len);
                }
                dataQueue.offer(buffData);
            } catch (Exception e) {
                isFailed = true;
                callback.onFailure(e, e.getMessage());
                service.shutdownNow();
            }
        }
    }

    class SaveTask implements Runnable {
        @Override
        public void run() {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(savePath.toAbsolutePath().toString(), "rw")) {
                long writSize = 0;
                do {
                    BufferData buffData = dataQueue.poll();
                    if (buffData == null) continue;
                    randomAccessFile.seek(buffData.getStartPos());
                    randomAccessFile.write(buffData.array());
                    writSize += buffData.array().length;
                } while (writSize < fileSize);
                isDone = true;
                stat.stop();
                callback.onSuccess(writSize);
            } catch (IOException e) {
                isFailed = true;
                callback.onFailure(e, e.getMessage());
                service.shutdownNow();
            }
        }
    }
}
