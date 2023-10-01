package me.heartalborada.biliDownloader.Bili.Beans;

import com.google.gson.Gson;
import lombok.Getter;
import okhttp3.Cookie;

import java.util.HashMap;
import java.util.List;

public class loginData {
    @Getter
    private final String refreshToken;
    @Getter
    private final HashMap<String, List<Cookie>> cookies;
    @Getter
    private final long timestamp;

    public loginData(String refreshToken, HashMap<String, List<Cookie>> cookies, long timestamp) {
        this.refreshToken = refreshToken;
        this.cookies = cookies;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
