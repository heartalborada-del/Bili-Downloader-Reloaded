package me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub;

import lombok.Data;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.SegmentBase;

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

}