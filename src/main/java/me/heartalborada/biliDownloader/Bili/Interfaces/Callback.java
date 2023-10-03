package me.heartalborada.biliDownloader.Bili.Interfaces;

import me.heartalborada.biliDownloader.Bili.Beans.loginData;

import java.util.Timer;

public interface Callback {

    void onSuccess(loginData data,String message,int code);

    void onFailure(Exception e, String cause,int code);

    void onUpdate(String message,int code);

    void onGetQRUrl(String QRUrl);
}
