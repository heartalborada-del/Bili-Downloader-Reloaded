package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DescV2 {

    @SerializedName("raw_text")
    private String rawText;
    private int type;
    @SerializedName("biz_id")
    private int bizId;
}