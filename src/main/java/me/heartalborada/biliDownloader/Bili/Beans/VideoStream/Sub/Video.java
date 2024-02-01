package me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub;

import lombok.Data;

import java.util.List;

@Data
public class Video {
    private int id;
    private String baseUrl;
    private List<String> backupUrl;
    private int bandwidth;
    private String mimeType;
    private String codecs;
    private int width;
    private int height;
    private String frameRate;
    private String sar;
    private int startWithSap;
    private SegmentBase segmentBase;
    private int codecid;
    public void setAll(Video v) {
        this.setId(v.getId());
        this.setBaseUrl(v.getBaseUrl());
        this.setBackupUrl(v.getBackupUrl());
        this.setBandwidth(v.getBandwidth());
        this.setMimeType(v.getMimeType());
        this.setCodecs(v.getCodecs());
        this.setWidth(v.getWidth());
        this.setHeight(v.getHeight());
        this.setFrameRate(v.getFrameRate());
        this.setSar(v.getSar());
        this.setStartWithSap(v.getStartWithSap());
        this.setSegmentBase(v.getSegmentBase());
        this.setCodecid(v.getCodecid());
    }
}