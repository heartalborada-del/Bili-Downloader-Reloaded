package me.heartalborada.biliDownloader.Bili;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.heartalborada.biliDownloader.Bili.bean.geetestVerify;
import me.heartalborada.biliDownloader.Bili.exceptions.BadRequestDataException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class biliInstance {
    private final int[] mixinKeyEncTab = new int[]{
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
            61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
            36, 20, 34, 44, 52
    };
    private final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new headerInterceptor()).build();
    private static class headerInterceptor implements Interceptor {

        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
            Request.Builder b = chain.request().newBuilder();
            b.addHeader("Origin", "https://www.bilibili.com/");
            b.addHeader("Referer","https://www.bilibili.com");
            b.addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.24");
            return chain.proceed(b.build());
        }
    }

    private String signature = upgradeWbiSign();

    public biliInstance() throws IOException {
        System.out.println(getNewGeetestCaptcha());
    }

    protected LinkedHashMap<String,String> signParameters(Map<String,String> parameters) throws IOException {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(parameters);
        copy.put("wts", String.valueOf(System.currentTimeMillis()/1000));
        StringJoiner paramStr = new StringJoiner("&");
        copy.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry ->
                                paramStr.add(entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                );
        if(signature == null)
            signature = upgradeWbiSign();
        copy.put("w_rid",generateMD5(paramStr + signature));
        return copy;
    }

    private static String generateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String upgradeWbiSign() throws IOException {
        Request req = new Request.Builder().url("https://api.bilibili.com/x/web-interface/nav").build();
        String imgAndSub;
        try (Response resp = client.newCall(req).execute()) {
            if (resp.body() != null) {
                String str = resp.body().string();
                JsonObject element = JsonParser.parseString(str).getAsJsonObject();
                JsonObject wbiData = element.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("wbi_img");
                String img = wbiData.getAsJsonPrimitive("img_url").getAsString();
                img = img.substring(img.lastIndexOf('/')+1,img.lastIndexOf("."));

                String sub = wbiData.getAsJsonPrimitive("sub_url").getAsString();
                sub = sub.substring(sub.lastIndexOf('/')+1,sub.lastIndexOf("."));
                imgAndSub = img+sub;
            } else {
                throw new IOException("Empty body");
            }
        }
        StringBuilder signatureTemp = new StringBuilder();
        for (int i : mixinKeyEncTab) {
            signatureTemp.append(imgAndSub.charAt(i));
        }
        return signatureTemp.substring(0,32);
    }

    public geetestVerify getNewGeetestCaptcha() throws IOException {
        Request req = new Request.Builder().url("https://passport.bilibili.com/x/passport-login/captcha?source=main_web").build();
        try (Response resp = client.newCall(req).execute()) {
            if (resp.body() != null) {
                String str = resp.body().string();
                JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                if(object.getAsJsonPrimitive("code").getAsInt() != 0) {
                    throw new BadRequestDataException(
                            object.getAsJsonPrimitive("code").getAsInt(),
                            object.getAsJsonPrimitive("message").getAsString()
                    );
                }
                object = object.getAsJsonObject("data");
                return new geetestVerify(
                    object.getAsJsonObject("geetest").getAsJsonPrimitive("challenge").getAsString(),
                    object.getAsJsonObject("geetest").getAsJsonPrimitive("gt").getAsString(),
                    object.getAsJsonPrimitive("token").getAsString()
                );
            } else {
                throw new IOException("Empty body");
            }
        }
    }
}
