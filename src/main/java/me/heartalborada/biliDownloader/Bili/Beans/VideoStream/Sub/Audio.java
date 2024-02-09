package me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Audio {
    private int id;
    @SerializedName("baseUrl")
    private String baseUrl;
    @SerializedName("backupUrl")
    private List<String> backupUrl;
    private int bandwidth;
    @SerializedName("mimeType")
    private String mimeType;
    private String codecs;
    private int width;
    private int height;
    @SerializedName("frameRate")
    private String frameRate;
    private String sar;
    @SerializedName("startWithSap")
    private int startWithSap;
    @SerializedName("SegmentBase")
    private SegmentBase segmentBase;
    private int codecid;

    public void setAll(Audio audio) {
        this.setId(audio.getId());
        this.setBaseUrl(audio.getBaseUrl());
        this.setBackupUrl(audio.getBackupUrl());
        this.setBandwidth(audio.getBandwidth());
        this.setMimeType(audio.getMimeType());
        this.setCodecs(audio.getCodecs());
        this.setWidth(audio.getWidth());
        this.setHeight(audio.getHeight());
        this.setFrameRate(audio.getFrameRate());
        this.setSar(audio.getSar());
        this.setStartWithSap(audio.getStartWithSap());
        this.setSegmentBase(audio.getSegmentBase());
        this.setCodecid(audio.getCodecid());
    }
}