package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Vip {

    private int type;
    private int status;
    @SerializedName("due_date")
    private int dueDate;
    @SerializedName("vip_pay_type")
    private int vipPayType;
    @SerializedName("theme_type")
    private int themeType;
    private Label label;
    @SerializedName("avatar_subscript")
    private int avatarSubscript;
    @SerializedName("nickname_color")
    private String nicknameColor;
    private int role;
    @SerializedName("avatar_subscript_url")
    private String avatarSubscriptUrl;
    @SerializedName("tv_vip_status")
    private int tvVipStatus;
    @SerializedName("tv_vip_pay_type")
    private int tvVipPayType;
}