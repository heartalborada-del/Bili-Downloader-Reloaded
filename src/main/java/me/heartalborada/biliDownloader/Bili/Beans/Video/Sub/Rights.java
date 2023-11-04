package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Rights {

    private int bp;
    private int elec;
    private int download;
    private int movie;
    private int pay;
    private int hd5;
    @SerializedName("no_reprint")
    private int noReprint;
    private int autoplay;
    @SerializedName("ugc_pay")
    private int ugcPay;
    @SerializedName("is_cooperation")
    private int isCooperation;
    @SerializedName("ugc_pay_preview")
    private int ugcPayPreview;
    @SerializedName("no_background")
    private int noBackground;
    @SerializedName("clean_mode")
    private int cleanMode;
    @SerializedName("is_stein_gate")
    private int isSteinGate;
    @SerializedName("is_360")
    private int is360;
    @SerializedName("no_share")
    private int noShare;
    @SerializedName("arc_pay")
    private int arcPay;
    @SerializedName("free_watch")
    private int freeWatch;
}