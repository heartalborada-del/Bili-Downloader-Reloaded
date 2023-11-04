package me.heartalborada.biliDownloader.Utils.Managers.Beans;

import lombok.Data;
import okhttp3.Cookie;

import java.util.HashMap;
import java.util.List;

@Data
public class DataInstance {
    private Bilibili bilibili = new Bilibili();

    @Data
    public static class Bilibili {
        private HashMap<String, List<Cookie>> cookies = new HashMap<>();
        private String refreshToken = "";
        private Long latestRefreshTimestamp = 0L;
    }
}
