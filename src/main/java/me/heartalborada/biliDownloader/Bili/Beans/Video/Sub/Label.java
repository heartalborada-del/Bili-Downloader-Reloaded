package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Label {

    private String path;
    private String text;
    @SerializedName("label_theme")
    private String labelTheme;
    @SerializedName("text_color")
    private String textColor;
    @SerializedName("bg_style")
    private int bgStyle;
    @SerializedName("bg_color")
    private String bgColor;
    @SerializedName("border_color")
    private String borderColor;
    @SerializedName("use_img_label")
    private boolean useImgLabel;
    @SerializedName("img_label_uri_hans")
    private String imgLabelUriHans;
    @SerializedName("img_label_uri_hant")
    private String imgLabelUriHant;
    @SerializedName("img_label_uri_hans_static")
    private String imgLabelUriHansStatic;
    @SerializedName("img_label_uri_hant_static")
    private String imgLabelUriHantStatic;

}