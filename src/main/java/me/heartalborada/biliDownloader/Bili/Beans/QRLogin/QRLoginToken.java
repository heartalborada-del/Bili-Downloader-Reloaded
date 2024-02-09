package me.heartalborada.biliDownloader.Bili.Beans.QRLogin;

import lombok.Getter;

@Getter
public class QRLoginToken {
    private final String QRUrl, Token;
    private final long RegTimestamp;

    public QRLoginToken(String qrUrl, String token, long regTimestamp) {
        QRUrl = qrUrl;
        Token = token;
        RegTimestamp = regTimestamp;
    }
}
