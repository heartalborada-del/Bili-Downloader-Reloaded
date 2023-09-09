package me.heartalborada.biliDownloader;

import com.google.gson.Gson;
import lombok.Getter;
import me.heartalborada.biliDownloader.Bili.bean.loginData;
import me.heartalborada.biliDownloader.Bili.biliInstance;
import me.heartalborada.biliDownloader.Bili.interfaces.Callback;
import me.heartalborada.biliDownloader.utils.librariesLoader;
import okhttp3.Cookie;
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
        list.add(new String[]{"org.jetbrains.kotlin","kotlin-stdlib-jdk8","1.6.20",""});
        list.add(new String[]{"org.jsoup","jsoup","1.16.1",""});

        try {
            for(String[] strs : list) {
                librariesLoader.loadLibraryClassMaven(
                        strs[0],strs[1],strs[2],strs[3], "https://maven.aliyun.com/repository/public", libPath
                );
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        testQrLogin();
    }

    public static void testQrLogin() throws IOException {
        biliInstance in = new biliInstance();
        in.new Login().new QR().loginWithQrLogin(new Callback() {
            @Override
            public void onSuccess(loginData data, String message, int code) {
                System.out.printf("%d-%s%n",code,message);
                System.out.println(data);
            }

            @Override
            public void onFailure(Exception e, String cause, int code) {
                System.out.printf("%d-%s%n",code,cause);
            }

            @Override
            public void onUpdate(String message, int code) {
                System.out.printf("%d-%s%n",code,message);
            }

            @Override
            public void onGetQRUrl(String QRUrl) {
                System.out.println(QRUrl);
            }
        });
    }
}
