package me.heartalborada.biliDownloader;

import lombok.Getter;
import me.heartalborada.biliDownloader.Cli.CliMain;
import me.heartalborada.biliDownloader.Utils.Managers.ConfigManager;
import me.heartalborada.biliDownloader.Utils.Managers.DataManager;
import me.heartalborada.biliDownloader.Utils.LibrariesLoader;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Main {
    @Getter
    private static final File dataPath = new File(System.getProperty("user.dir"),"data");
    @Getter
    private static final File libPath = new File(dataPath,"libs");
    @Getter
    private static ConfigManager configManager;
    @Getter
    private static DataManager dataManager;

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        if(!Objects.equals(System.getProperty("development","false"), "true")) {
            List<String[]> list = new LinkedList<>();

            list.add(new String[]{"com.squareup.okhttp3", "okhttp", "4.11.0", ""});
            list.add(new String[]{"com.google.code.gson", "gson", "2.10.1", ""});
            list.add(new String[]{"org.jetbrains.kotlin","kotlin-stdlib-jdk8","1.6.20",""});
            list.add(new String[]{"org.jsoup","jsoup","1.16.1",""});
            list.add(new String[]{"org.jetbrains","annotations","24.0.1",""});
            list.add(new String[]{"info.picocli", "picocli-shell-jline3", "4.7.5", ""});
            list.add(new String[]{"org.fusesource.jansi","jansi","2.4.0",""});
            list.add(new String[]{"com.google.zxing","core","3.5.2",""});

            try {
                for(String[] strs : list) {
                    LibrariesLoader.loadLibraryClassMaven(
                            strs[0],strs[1],strs[2],strs[3], "https://maven.aliyun.com/repository/public", libPath
                    );
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        configManager = new ConfigManager(new File(dataPath,"config.json"));
        dataManager = new DataManager(new File(dataPath,"data.json"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                configManager.save();
                dataManager.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        try {
            CliMain.main(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
