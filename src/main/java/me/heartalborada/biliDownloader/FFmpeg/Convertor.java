package me.heartalborada.biliDownloader.FFmpeg;

import me.heartalborada.biliDownloader.Interfaces.EncoderProgressListenerM;
import me.heartalborada.biliDownloader.Main;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Convertor {
    private static final ExecutorService service = Executors.newFixedThreadPool(5);

    public static Future<?> doConvertor(File video, File audio, File output, EncoderProgressListenerM listener) throws IOException {
        final Locator l = new Locator(Main.getConfigManager().getConfig().getFFmpegPath());
        final MultimediaObject vo = new MultimediaObject(video, l), ao = new MultimediaObject(audio, l);
        final Encoder encoder = new Encoder(l);
        return service.submit(() -> {
            try {
                EncodingAttributes encode = new EncodingAttributes();
                {
                    VideoAttributes va = new VideoAttributes();
                    va.setCodec(VideoAttributes.DIRECT_STREAM_COPY);
                    encode.setVideoAttributes(va);
                }
                {
                    AudioAttributes aa = new AudioAttributes();
                    aa.setCodec(VideoAttributes.DIRECT_STREAM_COPY);
                    encode.setAudioAttributes(aa);
                }
                encode.setOutputFormat(vo.getInfo().getFormat());
                encoder.encode(new LinkedList<>() {{
                    add(ao);
                    add(vo);
                }}, output, encode, listener);
            } catch (EncoderException e) {
                listener.onFailed(e);
            }
        });
    }
}
