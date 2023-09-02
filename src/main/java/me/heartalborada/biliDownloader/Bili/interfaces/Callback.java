package me.heartalborada.biliDownloader.Bili.interfaces;

import me.heartalborada.biliDownloader.Bili.bean.loginData;

public interface Callback {

    void onSuccess(loginData data,String message,int code);

    void onFailure(Exception e, String cause,int code);

    void onUpdate(String message,int code);

    void onGetQRUrl(String QRUrl);
}
