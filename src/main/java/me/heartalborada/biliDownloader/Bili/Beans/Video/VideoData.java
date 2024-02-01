package me.heartalborada.biliDownloader.Bili.Beans.Video;

import com.google.gson.annotations.SerializedName;
import me.heartalborada.biliDownloader.Bili.Beans.Video.Sub.*;

import java.util.List;

@lombok.Data
public class VideoData {
    private String bvid;
    private int aid;
    private int videos;
    private int tid;
    @SerializedName("tname")
    private String tagName;
    private int copyright;
    private String pic;
    private String title;
    @SerializedName("pubdate")
    private int publishDate;
    private int ctime;
    private String desc;
    @SerializedName("desc_v2")
    private List<DescV2> descV2;
    private int state;
    private long duration;
    @SerializedName("mission_id")
    private int missionId;
    private Rights rights;
    private Owner owner;
    private Stat stat;
    private String dynamic;
    private int cid;
    private Dimension dimension;
    private String premiere;
    private List<Pages> pages;
    private Subtitle subtitle;
    private List<Staff> staff;
    @SerializedName("honor_reply")
    private HonorReply honorReply;
}