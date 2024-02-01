package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Subtitle {

    @SerializedName("allow_submit")
    private boolean allowSubmit;
    private List<SubTitleListElement> list;
}