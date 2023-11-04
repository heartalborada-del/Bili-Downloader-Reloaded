package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Staff {

    private int mid;
    private String title;
    private String name;
    private String face;
    private Vip vip;
    private Official official;
    private int follower;
    @SerializedName("label_style")
    private int labelStyle;

}