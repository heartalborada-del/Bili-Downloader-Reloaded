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
    public void setAll(Pages p) {
        this.setCid(p.getCid());
        this.setPage(p.getPage());
        this.setFrom(p.getFrom());
        this.setPart(p.getPart());
        this.setDuration(p.getDuration());
        this.setVid(p.getVid());
        this.setWeblink(p.getWeblink());
        this.setDimension(p.getDimension());
    }
}