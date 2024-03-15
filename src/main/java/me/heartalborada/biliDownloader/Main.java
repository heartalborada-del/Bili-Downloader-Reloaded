package me.heartalborada.biliDownloader;

import lombok.Getter;
import me.heartalborada.biliDownloader.Bili.BiliInstance;
import me.heartalborada.biliDownloader.Cli.CliMain;
import me.heartalborada.biliDownloader.Utils.LibrariesLoader;
import me.heartalborada.biliDownloader.Utils.Managers.ConfigManager;
import me.heartalborada.biliDownloader.Utils.Managers.DataManager;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Main {
    @Getter
    private static final File dataPath = new File(System.getProperty("user.dir"), "data");
    @Getter
    private static final File libPath = new File(dataPath, "libs");
    @Getter
    private static final File cachePath = new File(dataPath, "caches");
    @Getter
    private static final File downloadPath = new File(dataPath, "downloads");
    @Getter
    private final static BiliInstance biliInstance;
    @Getter
    private static ConfigManager configManager;
    @Getter
    private static DataManager dataManager;

    static {
        BiliInstance temp;
        List<String[]> dependencelist = new LinkedList<>();
        if (!dataPath.exists() && !dataPath.mkdirs())
            throw new RuntimeException(new IOException(String.format("Failed create data dictionary: %s", dataPath.getPath())));
        try {
            if (!Objects.equals(System.getProperty("development", "false"), "true")) {
                boolean isBuiltinDependency = false;
                Enumeration<URL> resources = Main.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
                while (resources.hasMoreElements()) {
                    try {
                        Manifest manifest = new Manifest(resources.nextElement().openStream());
                        Attributes attributes = manifest.getMainAttributes();
                        isBuiltinDependency = attributes.getValue("Builtin-Dependency").equals("true");
                    } catch (IOException | NullPointerException E) {
                        isBuiltinDependency = false;
                    }
                }
                if (!isBuiltinDependency) {
                    dependencelist.add(new String[]{"com.squareup.okhttp3", "okhttp", "4.11.0", ""});
                    dependencelist.add(new String[]{"com.google.code.gson", "gson", "2.10.1", ""});
                    dependencelist.add(new String[]{"org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.6.20", ""});
                    dependencelist.add(new String[]{"org.jsoup", "jsoup", "1.16.1", ""});
                    dependencelist.add(new String[]{"org.jetbrains", "annotations", "24.0.1", ""});
                    dependencelist.add(new String[]{"info.picocli", "picocli-shell-jline3", "4.7.5", ""});
                    dependencelist.add(new String[]{"org.fusesource.jansi", "jansi", "2.4.0", ""});
                    dependencelist.add(new String[]{"com.google.zxing", "core", "3.5.2", ""});
                    dependencelist.add(new String[]{"org.apache.commons", "commons-exec", "1.4.0", ""});
                    dependencelist.add(new String[]{"org.slf4j", "slf4j-api", "2.0.11", ""});
                }
            }
            for (String[] strs : dependencelist) {
                try {
                    LibrariesLoader.loadLibraryClassMaven(
                            strs[0], strs[1], strs[2], strs[3], "https://maven.aliyun.com/repository/public", libPath
                    );
                } catch (IOException | SAXException | NoSuchAlgorithmException |
                         ParserConfigurationException e) {
                    throw new RuntimeException(e);
                }
            }
            configManager = new ConfigManager(new File(dataPath, "config.json"));
            dataManager = new DataManager(new File(dataPath, "data.json"));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    configManager.save();
                    dataManager.save();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
            temp = new BiliInstance(getDataManager().getData().getBilibili().getCookies());
        } catch (IOException e) {
            temp = new BiliInstance();
            //configManager = null;
        }
        biliInstance = temp;
    }

    public static void main(String[] args) throws IOException {
        CliMain.main(args);
    }
}
