package me.heartalborada.biliDownloader;

import lombok.Getter;
import me.heartalborada.biliDownloader.Bili.biliInstance;
import me.heartalborada.biliDownloader.utils.librariesLoader;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
public class Main {
    @Getter
    private static final File dataPath = new File(System.getProperty("user.dir"),"data");
    @Getter
    private static final File libPath = new File(dataPath,"libs");
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        List<String[]> list = new LinkedList<>();

        list.add(new String[]{"com.squareup.okhttp3", "okhttp", "4.11.0", ""});
        list.add(new String[]{"com.google.code.gson", "gson", "2.10.1", ""});
        try {
            for(String[] strs : list) {
                librariesLoader.loadLibraryClassMaven(
                        strs[0],strs[1],strs[2],strs[3], "https://maven.aliyun.com/repository/public", libPath
                );
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        new biliInstance();
    }
}
