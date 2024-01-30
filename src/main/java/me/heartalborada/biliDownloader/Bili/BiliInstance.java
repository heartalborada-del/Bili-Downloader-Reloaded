package me.heartalborada.biliDownloader.Bili;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Data;
import lombok.Getter;
import me.heartalborada.biliDownloader.Bili.Beans.CountrySMS;
import me.heartalborada.biliDownloader.Bili.Beans.GeetestVerify;
import me.heartalborada.biliDownloader.Bili.Beans.LoginData;
import me.heartalborada.biliDownloader.Bili.Beans.QRLoginToken;
import me.heartalborada.biliDownloader.Bili.Beans.Video.VideoData;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.VideoStreamData;
import me.heartalborada.biliDownloader.Bili.Exceptions.BadRequestDataException;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Utils.Okhttp.SimpleCookieJar;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "Duplicates"})
public class BiliInstance {
    private final int[] mixinKeyEncTab = new int[]{
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
            61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
            36, 20, 34, 44, 52
    };
    private final SimpleCookieJar simpleCookieJar;
    private final OkHttpClient client;
    @Getter
    private final Login login = new Login();
    @Getter
    private final Account account = new Account();
    @Getter
    private final Video video = new Video();
    private final Signature signature = new Signature();

    public BiliInstance() {
        simpleCookieJar = new SimpleCookieJar();
        client = new OkHttpClient.Builder()
                .addInterceptor(new headerInterceptor())
                .cookieJar(simpleCookieJar)
                .build();
    }

    public BiliInstance(HashMap<String, List<Cookie>> CookieData) throws IOException {
        simpleCookieJar = new SimpleCookieJar(CookieData);
        client = new OkHttpClient.Builder()
                .addInterceptor(new headerInterceptor())
                .cookieJar(simpleCookieJar)
                .build();
    }

    public GeetestVerify getNewGeetestCaptcha() throws IOException {
        Request req = new Request.Builder().url("https://passport.bilibili.com/x/passport-login/captcha?source=main_web").build();
        try (Response resp = client.newCall(req).execute()) {
            if (resp.body() != null) {
                String str = resp.body().string();
                JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                    throw new BadRequestDataException(
                            object.getAsJsonPrimitive("code").getAsInt(),
                            object.getAsJsonPrimitive("message").getAsString()
                    );
                }
                object = object.getAsJsonObject("data");
                return new GeetestVerify(
                        object.getAsJsonObject("geetest").getAsJsonPrimitive("challenge").getAsString(),
                        object.getAsJsonObject("geetest").getAsJsonPrimitive("gt").getAsString(),
                        object.getAsJsonPrimitive("token").getAsString()
                );
            } else {
                throw new BadRequestDataException(resp.code(), "The url has no data");
            }
        }
    }

    private static class headerInterceptor implements Interceptor {

        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
            Request.Builder b = chain.request().newBuilder();
            b.addHeader("Origin", "https://www.bilibili.com/");
            b.addHeader("Referer", "https://www.bilibili.com");
            b.addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36 Edg/111.0.1661.24");
            return chain.proceed(b.build());
        }
    }

    @Data
    private class Signature {

        private String signStr;
        private long expireTimeStamp;

        public void upgrade() throws IOException {
            Request req = new Request.Builder().url("https://api.bilibili.com/x/web-interface/nav").build();
            String imgAndSub;
            try (Response resp = client.newCall(req).execute()) {
                if (resp.body() != null) {
                    String str = resp.body().string();
                    JsonObject element = JsonParser.parseString(str).getAsJsonObject();
                    JsonObject wbiData = element.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("wbi_img");
                    String img = wbiData.getAsJsonPrimitive("img_url").getAsString();
                    img = img.substring(img.lastIndexOf('/') + 1, img.lastIndexOf("."));

                    String sub = wbiData.getAsJsonPrimitive("sub_url").getAsString();
                    sub = sub.substring(sub.lastIndexOf('/') + 1, sub.lastIndexOf("."));
                    imgAndSub = img + sub;
                } else {
                    throw new BadRequestDataException(resp.code(), "The url has no data");
                }
            }
            StringBuilder signatureTemp = new StringBuilder();
            for (int i : mixinKeyEncTab) {
                signatureTemp.append(imgAndSub.charAt(i));
            }
            signStr = signatureTemp.substring(0, 32);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            expireTimeStamp = calendar.getTimeInMillis();
        }

        private String generateMD5(String input) {
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

        protected TreeMap<String, String> sign(Map<String, String> parameters) throws IOException {
            TreeMap<String, String> copy = new TreeMap<>(parameters);
            copy.put("wts", String.valueOf(System.currentTimeMillis() / 1000));
            StringJoiner paramStr = new StringJoiner("&");
            copy.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(
                            entry ->
                                    paramStr.add(entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    );
            if (signStr == null || System.currentTimeMillis() >= expireTimeStamp)
                upgrade();
            copy.put("w_rid", generateMD5(paramStr + signStr));
            return copy;
        }

    }

    public class Login {
        @Getter
        private final SMS SMS = new SMS();
        @Getter
        private final Password Password = new Password();
        @Getter
        private final QR QR = new QR();

        public class SMS {
            public LinkedList<CountrySMS> getCountryList() throws IOException {
                Request req = new Request.Builder().url("https://passport.bilibili.com/web/generic/country/list").build();
                LinkedList<CountrySMS> list = new LinkedList<>();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.body() != null) {
                        String str = resp.body().string();
                        JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                        if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                            throw new BadRequestDataException(
                                    object.getAsJsonPrimitive("code").getAsInt(),
                                    object.getAsJsonPrimitive("message").getAsString()
                            );
                        }
                        JsonObject data = object.getAsJsonObject("data");
                        for (JsonElement o : data.getAsJsonArray("common")) {
                            JsonObject o1 = o.getAsJsonObject();
                            list.add(new CountrySMS(
                                    o1.getAsJsonPrimitive("id").getAsInt(),
                                    o1.getAsJsonPrimitive("cname").getAsString(),
                                    o1.getAsJsonPrimitive("country_id").getAsInt()
                            ));
                        }
                        for (JsonElement o : data.getAsJsonArray("others")) {
                            JsonObject o1 = o.getAsJsonObject();
                            list.add(new CountrySMS(
                                    o1.getAsJsonPrimitive("id").getAsInt(),
                                    o1.getAsJsonPrimitive("cname").getAsString(),
                                    o1.getAsJsonPrimitive("country_id").getAsInt()
                            ));
                        }
                    } else {
                        throw new BadRequestDataException(resp.code(), "The url has no data");
                    }
                }
                return list;
            }

            public String sendSMSCode(long tel, int countryID, GeetestVerify verify) throws IOException {
                RequestBody body = new FormBody.Builder()
                        .add("cid", String.valueOf(countryID))
                        .add("tel", String.valueOf(tel))
                        .add("source", "main_web")
                        .add("token", verify.getToken())
                        .add("challenge", verify.getChallenge())
                        .add("validate", verify.getValidate())
                        .add("seccode", verify.getSeccode())
                        .build();
                Request req = new Request.Builder()
                        .post(body)
                        .url("https://passport.bilibili.com/x/passport-login/web/sms/send")
                        .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.body() != null) {
                        String str = resp.body().string();
                        JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                        if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                            throw new BadRequestDataException(
                                    object.getAsJsonPrimitive("code").getAsInt(),
                                    object.getAsJsonPrimitive("message").getAsString()
                            );
                        }
                        return object.getAsJsonObject("data").getAsJsonPrimitive("captcha_key").getAsString();
                    } else {
                        throw new BadRequestDataException(resp.code(), "The url has no data");
                    }
                }
            }

            public LoginData loginWithSMS(String SMSToken, long tel, int countryID, int SMSCode) throws IOException {
                RequestBody body = new FormBody.Builder()
                        .add("cid", String.valueOf(countryID))
                        .add("tel", String.valueOf(tel))
                        .add("code", String.valueOf(SMSCode))
                        .add("source", "main_web")
                        .add("captcha_key", SMSToken)
                        .add("go_url", "https://www.bilibili.com")
                        .build();
                Request req = new Request.Builder()
                        .post(body)
                        .url("https://passport.bilibili.com/x/passport-login/web/login/sms")
                        .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.body() != null) {
                        String str = resp.body().string();
                        JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                        if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                            throw new BadRequestDataException(
                                    object.getAsJsonPrimitive("code").getAsInt(),
                                    object.getAsJsonPrimitive("message").getAsString()
                            );
                        }
                        String RT = object.getAsJsonObject("data").getAsJsonPrimitive("refresh_token").getAsString();
                        long TS = object.getAsJsonObject("data").getAsJsonPrimitive("timestamp").getAsLong();

                        return new LoginData(RT, simpleCookieJar.getCookieStore(), TS);
                    } else {
                        throw new BadRequestDataException(resp.code(), "The url has no data");
                    }
                }
            }
        }

        public class Password {
            private keys getSaltAndKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
                Request req = new Request.Builder()
                        .url("https://passport.bilibili.com/x/passport-login/web/key")
                        .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.body() != null) {
                        String str = resp.body().string();
                        JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                        if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                            throw new BadRequestDataException(
                                    object.getAsJsonPrimitive("code").getAsInt(),
                                    object.getAsJsonPrimitive("message").getAsString()
                            );
                        }
                        String salt = object.getAsJsonObject("data").getAsJsonPrimitive("hash").getAsString();
                        String keyS = object.getAsJsonObject("data").getAsJsonPrimitive("key").getAsString();

                        return new keys(salt,
                                (RSAPublicKey) KeyFactory.getInstance("RSA")
                                        .generatePublic(
                                                new X509EncodedKeySpec(
                                                        Base64.getDecoder().decode(keyS)
                                                )
                                        )
                        );
                    } else {
                        throw new BadRequestDataException(resp.code(), "The url has no data");
                    }
                }
            }

            public LoginData loginWithPassword(String username, String password, GeetestVerify verify) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
                keys k = getSaltAndKey();
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, k.getKey());
                String encryptPw = Base64.getEncoder().encodeToString(cipher.doFinal((k.getSalt() + password).getBytes(StandardCharsets.UTF_8)));

                RequestBody body = new FormBody.Builder()
                        .add("username", username)
                        .add("password", encryptPw)
                        .add("keep", "0")
                        .add("token", verify.getToken())
                        .add("challenge", verify.getChallenge())
                        .add("validate", verify.getValidate())
                        .add("seccode", verify.getSeccode())
                        .add("go_url", "https://www.bilibili.com")
                        .add("source", "main_web")
                        .build();
                Request req = new Request.Builder()
                        .post(body)
                        .url("https://passport.bilibili.com/x/passport-login/web/login")
                        .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (resp.body() != null) {
                        String str = resp.body().string();
                        JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                        if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                            throw new BadRequestDataException(
                                    object.getAsJsonPrimitive("code").getAsInt(),
                                    object.getAsJsonPrimitive("message").getAsString()
                            );
                        }
                        String RT = object.getAsJsonObject("data").getAsJsonPrimitive("refresh_token").getAsString();
                        long TS = object.getAsJsonObject("data").getAsJsonPrimitive("timestamp").getAsLong();

                        return new LoginData(RT, simpleCookieJar.getCookieStore(), TS);
                    } else {
                        throw new BadRequestDataException(resp.code(), "The url has no data");
                    }
                }
            }

            private class keys {
                @Getter
                private final String salt;
                @Getter
                private final RSAPublicKey key;

                private keys(String salt, RSAPublicKey key) {
                    this.salt = salt;
                    this.key = key;
                }
            }
        }

        public class QR {
            public QRLoginToken getQRLoginToken() throws BadRequestDataException, IOException {
                try (Response resp = client
                        .newCall(
                                new Request.Builder()
                                        .url("https://passport.bilibili.com/x/passport-login/web/qrcode/generate?source=main-fe-header")
                                        .build()
                        ).execute()) {
                    if (resp.body() != null) {
                        JsonObject object = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                        if (object.getAsJsonPrimitive("code").getAsInt() != 0)
                            throw new BadRequestDataException(
                                    object.getAsJsonPrimitive("code").getAsInt(),
                                    object.getAsJsonPrimitive("message").getAsString()
                            );
                        return new QRLoginToken(
                                object.getAsJsonObject("data").getAsJsonPrimitive("url").getAsString(),
                                object.getAsJsonObject("data").getAsJsonPrimitive("qrcode_key").getAsString()
                        );
                    } else {
                        throw new BadRequestDataException(resp.code(), "The url has no data");
                    }
                }
            }

            public ScheduledFuture<?> loginWithQrLogin(QRLoginToken token, Callback callback) {
                ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                return service.scheduleAtFixedRate(()->{
                    String tk = token.getToken();
                    try (Response response = client.newCall(new Request.Builder()
                            .url(
                                    String.format(
                                            "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=%s",
                                            tk
                                    )
                            ).build()).execute()) {
                        if (response.body() != null) {
                            JsonObject object = JsonParser.parseString(response.body().string()).getAsJsonObject();
                            int code = object.getAsJsonObject("data").getAsJsonPrimitive("code").getAsInt();
                            String msg = object.getAsJsonObject("data").getAsJsonPrimitive("message").getAsString();
                            switch (code){
                                case 0:
                                    long ts = object.getAsJsonObject("data").getAsJsonPrimitive("timestamp").getAsLong();
                                    String rt = object.getAsJsonObject("data").getAsJsonPrimitive("refresh_token").getAsString();
                                    callback.onSuccess(
                                            new LoginData(rt, simpleCookieJar.getCookieStore(), ts),
                                            msg,
                                            code
                                    );
                                    break;
                                case 86101:
                                case 86090:
                                    callback.onUpdate(msg, code);
                                    break;
                                case 86038:
                                    callback.onFailure(new BadRequestDataException(code, msg), msg, code);
                                    break;
                            }
                        } else {
                            throw new BadRequestDataException(response.code(), "The url has no data");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },0,1, TimeUnit.SECONDS);
            }
        }
    }

    public class Account {
        public boolean isNeedRefreshToken() throws IOException {
            Request req = new Request.Builder().url("https://passport.bilibili.com/x/passport-login/web/cookie/info").build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.body() != null) {
                    String str = resp.body().string();
                    JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                    if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                        throw new BadRequestDataException(
                                object.getAsJsonPrimitive("code").getAsInt(),
                                object.getAsJsonPrimitive("message").getAsString()
                        );
                    }
                    object = object.getAsJsonObject("data");
                    return object.getAsJsonPrimitive("refresh").getAsBoolean();
                } else {
                    throw new BadRequestDataException(resp.code(), "The url has no data");
                }
            }
        }

        public String getCsrfToken(String correspondPath) throws IOException {
            Request req = new Request.Builder().url(String.format("https://www.bilibili.com/correspond/1/%s", correspondPath)).build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.body() != null) {
                    Document docDesc = Jsoup.parse(resp.body().string());
                    Element e = docDesc.getElementById("1-name");
                    if (e != null) {
                        return e.text();
                    }
                    return null;
                } else {
                    throw new BadRequestDataException(resp.code(), "The url has no data");
                }
            }
        }

        /**
         * @param csrf         Cookie -> bili_jct
         * @param refreshCsrf  Csrf Token
         * @param refreshToken Refresh Token
         * @return New Cookie and refresh token
         */
        public LoginData refreshCookie(String csrf, String refreshCsrf, String refreshToken) throws IOException {
            RequestBody body = new FormBody.Builder()
                    .add("csrf", csrf)
                    .add("refresh_csrf", refreshCsrf)
                    .add("source", "main_web")
                    .add("refresh_token", refreshToken)
                    .build();
            Request req = new Request.Builder()
                    .post(body)
                    .url("https://passport.bilibili.com/x/passport-login/web/cookie/refresh")
                    .build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.body() != null) {
                    String str = resp.body().string();
                    JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                    if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                        throw new BadRequestDataException(
                                object.getAsJsonPrimitive("code").getAsInt(),
                                object.getAsJsonPrimitive("message").getAsString()
                        );
                    }
                    return new LoginData(
                            object.getAsJsonObject("data").getAsJsonPrimitive("refresh_token").getAsString(),
                            simpleCookieJar.getCookieStore(),
                            System.currentTimeMillis()
                    );
                } else {
                    throw new BadRequestDataException(resp.code(), "The url has no data");
                }
            }
        }

        /**
         * @param csrf            Cookie -> bili_jct
         * @param oldRefreshToken Old Refresh Token
         */
        public void setOldCookieInvalid(String csrf, String oldRefreshToken) throws IOException {
            RequestBody body = new FormBody.Builder()
                    .add("csrf", csrf)
                    .add("source", "main_web")
                    .add("refresh_token", oldRefreshToken)
                    .build();
            Request req = new Request.Builder()
                    .post(body)
                    .url("https://passport.bilibili.com/x/passport-login/web/confirm/refresh")
                    .build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.body() != null) {
                    String str = resp.body().string();
                    JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                    if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                        throw new BadRequestDataException(
                                object.getAsJsonPrimitive("code").getAsInt(),
                                object.getAsJsonPrimitive("message").getAsString()
                        );
                    }
                } else {
                    throw new BadRequestDataException(resp.code(), "The url has no data");
                }
            }
        }
    }

    @SuppressWarnings("Duplicates")
    public class Video {
        public VideoData getVideoData(int aid) throws IOException, BadRequestDataException {
            Request req = new Request.Builder().url(String.format("https://api.bilibili.com/x/web-interface/view?aid=%d", aid)).build();
            try (Response response = client.newCall(req).execute()) {
                if (response.body() != null) {
                    String str = response.body().string();
                    JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                    if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                        throw new BadRequestDataException(
                                object.getAsJsonPrimitive("code").getAsInt(),
                                object.getAsJsonPrimitive("message").getAsString()
                        );
                    }
                    return new Gson().fromJson(object.get("data"), VideoData.class);
                } else {
                    throw new BadRequestDataException(response.code(), "The url has no data");
                }
            }
        }

        public VideoData getVideoData(String bvid) throws IOException {
            Request req = new Request.Builder().url(String.format("https://api.bilibili.com/x/web-interface/view?bvid=%s", bvid)).build();
            try (Response response = client.newCall(req).execute()) {
                if (response.body() != null) {
                    String str = response.body().string();
                    JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                    if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                        throw new BadRequestDataException(
                                object.getAsJsonPrimitive("code").getAsInt(),
                                object.getAsJsonPrimitive("message").getAsString()
                        );
                    }
                    return new Gson().fromJson(object.get("data"), VideoData.class);
                } else {
                    throw new BadRequestDataException(response.code(), "The url has no data");
                }
            }
        }

        public VideoStreamData getVideoStreamData(VideoData data, int page) throws IOException {
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse("https://api.bilibili.com/x/player/wbi/playurl")).newBuilder();
            Map<String, String> param = new HashMap<>();
            param.put("bvid", data.getBvid());
            param.put("cid", String.valueOf(data.getPages().get(page).getCid()));
            param.put("fourk", "1");
            param.put("fnval", "16");
            TreeMap<String, String> signedParam = signature.sign(param);
            for (String key : signedParam.keySet()) {
                urlBuilder.addQueryParameter(key, signedParam.get(key));
            }
            Request req = new Request.Builder().url(urlBuilder.build()).build();
            try (Response response = client.newCall(req).execute()) {
                if (response.body() != null) {
                    String str = response.body().string();
                    JsonObject object = JsonParser.parseString(str).getAsJsonObject();
                    if (object.getAsJsonPrimitive("code").getAsInt() != 0) {
                        throw new BadRequestDataException(
                                object.getAsJsonPrimitive("code").getAsInt(),
                                object.getAsJsonPrimitive("message").getAsString()
                        );
                    }
                    return new Gson().fromJson(object.get("data"), VideoStreamData.class);
                } else {
                    throw new BadRequestDataException(response.code(), "The url has no data");
                }
            }
        }
    }
}
