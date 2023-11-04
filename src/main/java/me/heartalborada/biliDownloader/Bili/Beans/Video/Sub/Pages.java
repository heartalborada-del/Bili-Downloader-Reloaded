package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import lombok.Data;

@Data
public class Pages {

    private int cid;
    private int page;
    private String from;
    private String part;
    private int duration;
    private String vid;
    private String weblink;
    private Dimension dimension;
}