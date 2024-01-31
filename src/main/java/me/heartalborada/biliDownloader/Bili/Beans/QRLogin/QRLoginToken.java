package me.heartalborada.biliDownloader.Bili.Beans.QRLogin;

import lombok.Getter;

public class QRLoginToken {
    @Getter
    private final String QRUrl,Token;
    @Getter
    private final long RegTimestamp;
    public QRLoginToken(String qrUrl, String token, long regTimestamp) {
        QRUrl = qrUrl;
        Token = token;
        RegTimestamp = regTimestamp;
    }
}
