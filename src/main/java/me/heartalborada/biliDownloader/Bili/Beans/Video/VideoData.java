package me.heartalborada.biliDownloader.Bili.Beans.Video;

import lombok.Getter;

import java.util.LinkedList;

@Getter
public class VideoData {
    String bvid;
    String aid;
    int videos;
    long publishTime;
    long uploadTime;
    LinkedList<VideoDescription> description;
}
