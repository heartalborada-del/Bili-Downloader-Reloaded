package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Stat {

    private int aid;
    private int view;
    private int danmaku;
    private int reply;
    private int favorite;
    private int coin;
    private int share;
    @SerializedName("now_rank")
    private int nowRank;
    @SerializedName("his_rank")
    private int hisRank;
    private int like;
    private int dislike;
    private String evaluation;
    @SerializedName("argue_msg")
    private String argueMsg;


}