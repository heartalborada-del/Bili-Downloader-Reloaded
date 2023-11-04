package me.heartalborada.biliDownloader.Bili.Beans.VideoStream;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.Dash;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.Durl;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.SupportFormats;

@Data
public class VideoStreamData {

    private String from;
    private String result;
    private String message;
    private int quality;
    private String format;
    private int timelength;
    @SerializedName("accept_format")
    private String acceptFormat;
    @SerializedName("accept_description")
    private List<String> acceptDescription;
    @SerializedName("accept_quality")
    private List<Integer> acceptQuality;
    @SerializedName("video_codecid")
    private int videoCodecid;
    @SerializedName("seek_param")
    private String seekParam;
    @SerializedName("seek_type")
    private String seekType;
    private List<Durl> durl;
    @SerializedName("support_formats")
    private List<SupportFormats> supportFormats;
    @SerializedName("high_format")
    private String highFormat;
    @SerializedName("last_play_time")
    private int lastPlayTime;
    @SerializedName("last_play_cid")
    private int lastPlayCid;
    private Dash dash;
}