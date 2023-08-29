package me.heartalborada.biliDownloader.Bili.bean;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

public class geetestVerify {
    @Getter
    private final String challenge;
    @Getter
    private final String gt;
    @Getter
    private final String token;
    @Getter
    @Setter
    private String validate;
    @Getter
    @Setter
    private String seccode;

    public geetestVerify(String challenge, String gt, String token) {
        this.challenge = challenge;
        this.gt = gt;
        this.token = token;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
