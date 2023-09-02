package me.heartalborada.biliDownloader.Bili.bean;

import com.google.gson.JsonElement;
import lombok.Getter;

public class loginData {
    @Getter
    private final String refreshToken;
    @Getter
    private final JsonElement cookies;
    @Getter
    private final long timestamp;

    public loginData(String refreshToken, JsonElement cookies, long timestamp) {
        this.refreshToken = refreshToken;
        this.cookies = cookies;
        this.timestamp = timestamp;
    }
}
