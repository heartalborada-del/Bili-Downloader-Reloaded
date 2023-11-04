package me.heartalborada.biliDownloader;


import me.heartalborada.biliDownloader.MultiThreadDownload.DownloadInstance;
import me.heartalborada.biliDownloader.MultiThreadDownload.MultiThreadDownloader;
import me.heartalborada.biliDownloader.MultiThreadDownload.Speed.DownloadSpeedStat;
import me.heartalborada.biliDownloader.MultiThreadDownload.Speed.SpeedNotifyEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static me.heartalborada.biliDownloader.Utils.Util.byteToUnit;

public class Test {

    public static void main(String[] args) throws IOException {
        new DownloadInstance(
                10 * 1024 * 1024,
                8,
                new URL("https://mirror.tuna.tsinghua.edu.cn/Adoptium/19/jdk/x64/windows/OpenJDK19U-jdk_x64_windows_hotspot_19.0.2_7.msi"),
                new MultiThreadDownloader.Callback() {
                    @Override
                    public void onSuccess(long fileSize) {
                        System.out.println("Done");
                    }

                    @Override
                    public void onFailure(Exception e, String cause) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onStart(long fileSize) {
                        System.out.printf("Size: %s%n",byteToUnit(fileSize));
                    }
                },
                Path.of("C:\\Users\\heart\\Downloads\\Music\\new.msi"),
                new DownloadSpeedStat(speed -> System.out.printf("Speed: %s%n",byteToUnit(speed)))
        );
    }
}
