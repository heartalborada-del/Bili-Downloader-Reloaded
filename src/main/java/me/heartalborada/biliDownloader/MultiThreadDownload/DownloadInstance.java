package me.heartalborada.biliDownloader.MultiThreadDownload;

import me.heartalborada.biliDownloader.MultiThreadDownload.Speed.DownloadSpeedStat;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

public class DownloadInstance {
    private final URL url;
    private final ExecutorService service;
    private final BlockingQueue<BufferData> dataQueue = new PriorityBlockingQueue<>();
    private final long fileSize;
    private final MultiThreadDownloader.Callback callback;
    private final Path savePath;
    private final DownloadSpeedStat stat;
    private long THREAD_COUNTER = 0;
    public DownloadInstance(
            long threshold,
            int threadCount,
            URL url,
            MultiThreadDownloader.Callback callback,
            Path savePath,
            DownloadSpeedStat stat
    ) throws IOException {
        this.stat = stat;
        service = Executors.newFixedThreadPool(threadCount+1);
        this.url = url;
        this.callback = callback;
        this.savePath = savePath;
        URLConnection conn = url.openConnection();
        long length = -1;
        boolean isAllowMultiThread = false;
        if (!Objects.equals(conn.getHeaderField("Transfer-Encoding"), "chunked") && Objects.equals(conn.getHeaderField("Accept-Ranges"), "bytes")) {
            length = conn.getContentLength();
            isAllowMultiThread = true;
        }
        conn.getInputStream().close();
        this.fileSize = length;
        callback.onStart(fileSize);
        service.submit(new SaveTask());
        if(isAllowMultiThread && fileSize > threshold) {
            long startPos = 0, endPos = 0;
            long count = fileSize / threshold;
            for (long i = 0; i < count; i++) {
                startPos = i * threshold;
                endPos = startPos + threshold - 1;
                service.submit(new Task(startPos,endPos));
            }
            if (endPos < fileSize - 1) {
                service.submit(new Task(endPos+1, fileSize - 1));
            }
        } else {
            service.submit(new Task(0,fileSize));
        }
        stat.start();
    }

    class Task implements Runnable{
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
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Range",String.format("bytes=%d-%d",startPos,endPos));
                conn.connect();
                BufferData buffData = new BufferData(serialNum, startPos, endPos);
                byte[] data = new byte[1024 * 8];
                int len;
                InputStream inputStream = conn.getInputStream();
                if (inputStream != null) {
                    while ((len = inputStream.read(data)) > 0) {
                        stat.add(len);
                        buffData.write(data, 0, len);
                    }
                }
                dataQueue.offer(buffData);
            } catch (Exception e) {
                callback.onFailure(e,e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    class SaveTask implements Runnable {
        @Override
        public void run() {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(savePath.toAbsolutePath().toString(), "rw")) {
                long writSize = 0;
                do {
                    BufferData buffData = dataQueue.take();
                    randomAccessFile.seek(buffData.getStartPos());
                    randomAccessFile.write(buffData.array());
                    //log.info(buffData.getStartPos() + "-" + buffData.getEndPos() + " 已写入到文件，写入长度：" + buffData.array().length);
                    writSize += buffData.array().length;
                } while (writSize < fileSize);
                stat.stop();
                callback.onSuccess(writSize);
            } catch (IOException | InterruptedException e) {
                callback.onFailure(e,e.getMessage());
            }
        }
    }
}
