package me.heartalborada.biliDownloader;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.Getter;
import me.heartalborada.biliDownloader.Bili.Beans.loginData;
import me.heartalborada.biliDownloader.Bili.biliInstance;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Cli.CliMain;
import me.heartalborada.biliDownloader.Utils.Managers.configManager;
import me.heartalborada.biliDownloader.Utils.Managers.dataManager;
import me.heartalborada.biliDownloader.Utils.librariesLoader;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Main {
    @Getter
    private static final File dataPath = new File(System.getProperty("user.dir"),"data");
    @Getter
    private static final File libPath = new File(dataPath,"libs");
    @Getter
    private static configManager Config;
    @Getter
    private static dataManager Data;

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        if(!Objects.equals(System.getProperty("development","false"), "true")) {
            List<String[]> list = new LinkedList<>();

            list.add(new String[]{"com.squareup.okhttp3", "okhttp", "4.11.0", ""});
            list.add(new String[]{"com.google.code.gson", "gson", "2.10.1", ""});
            list.add(new String[]{"org.jetbrains.kotlin","kotlin-stdlib-jdk8","1.6.20",""});
            list.add(new String[]{"org.jsoup","jsoup","1.16.1",""});
            list.add(new String[]{"org.jetbrains","annotations","24.0.1",""});
            list.add(new String[]{"org.jline", "jline", "3.23.0", ""});
            list.add(new String[]{"org.fusesource.jansi","jansi","2.4.0",""});
            list.add(new String[]{"com.google.zxing","core","3.5.2",""});
            try {
                for(String[] strs : list) {
                    librariesLoader.loadLibraryClassMaven(
                            strs[0],strs[1],strs[2],strs[3], "https://maven.aliyun.com/repository/public", libPath
                    );
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        Config = new configManager(new File(dataPath,"config.json"));
        Data = new dataManager(new File(dataPath,"data.json"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Config.save();
                Data.save();
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
