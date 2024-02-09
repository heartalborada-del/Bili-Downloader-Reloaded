package me.heartalborada.biliDownloader.Bili.Beans.VideoStream;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.Dash;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.Durl;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.SupportFormats;

import java.util.List;

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

    public void setAll(VideoStreamData data) {
        this.setFrom(data.getFrom());
        this.setResult(data.getResult());
        this.setMessage(data.getMessage());
        this.setQuality(data.getQuality());
        this.setFormat(data.getFormat());
        this.setTimelength(data.getTimelength());
        this.setAcceptFormat(data.getAcceptFormat());
        this.setAcceptDescription(data.getAcceptDescription());
        this.setAcceptQuality(data.getAcceptQuality());
        this.setVideoCodecid(data.getVideoCodecid());
        this.setSeekParam(data.getSeekParam());
        this.setSeekType(data.getSeekType());
        this.setDurl(data.getDurl());
        this.setSupportFormats(data.getSupportFormats());
        this.setHighFormat(data.getHighFormat());
        this.setLastPlayTime(data.getLastPlayTime());
        this.setLastPlayCid(data.getLastPlayCid());
        this.setDash(data.getDash());
    }
}