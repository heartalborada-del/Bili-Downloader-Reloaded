package me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Durl {

    private int order;
    private int length;
    private int size;
    private String ahead;
    private String vhead;
    private String url;
    @SerializedName("backup_url")
    private List<String> backupUrl;
}