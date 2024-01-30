package me.heartalborada.biliDownloader.Bili.Beans;

import lombok.Getter;

public class QRLoginToken {
    @Getter
    private final String QRUrl,Token;
    public QRLoginToken(String qrUrl, String token) {
        QRUrl = qrUrl;
        Token = token;
    }
}
