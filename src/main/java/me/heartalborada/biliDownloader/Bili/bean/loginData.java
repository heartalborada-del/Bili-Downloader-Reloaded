package me.heartalborada.biliDownloader.Bili.bean;

import lombok.Getter;

import java.util.LinkedList;

public class loginData {
    @Getter
    private final String refreshToken;
    @Getter
    private final String cookies;
    @Getter
    private final long timestamp;

    public loginData(String refreshToken, String cookies, long timestamp) {
        this.refreshToken = refreshToken;
        this.cookies = cookies;
        this.timestamp = timestamp;
    }
}
