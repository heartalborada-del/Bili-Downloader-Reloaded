package me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub;

import lombok.Data;

import java.util.List;

@Data
public class Dash {

    private int duration;
    private double minBufferTime;
    private List<Video> video;
    private List<Audio> audio;
    private Dolby dolby;
    private Flac flac;
}