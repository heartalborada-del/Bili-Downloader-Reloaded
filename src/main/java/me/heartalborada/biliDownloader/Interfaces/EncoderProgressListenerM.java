package me.heartalborada.biliDownloader.Interfaces;

import ws.schild.jave.progress.EncoderProgressListener;

public interface EncoderProgressListenerM extends EncoderProgressListener {
    void onFailed(Throwable throwable);
}
