package me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class SupportFormats {
    private int quality;
    private String format;
    @SerializedName("new_description")
    private String newDescription;
    @SerializedName("display_desc")
    private String displayDesc;
    private String superscript;
    private List<String> codecs;
}