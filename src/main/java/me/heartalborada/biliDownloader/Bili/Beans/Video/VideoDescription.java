package me.heartalborada.biliDownloader.Bili.Beans.Video;

import lombok.Getter;

@Getter
public class VideoDescription {
    private final boolean isAt;
    private final long mid;
    private final String rawText;
    VideoDescription(boolean isAt, long mid, String rawText) {
        this.isAt = isAt;
        this.mid = mid;
        this.rawText = rawText;
    }
}
