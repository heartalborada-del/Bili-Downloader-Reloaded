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

}