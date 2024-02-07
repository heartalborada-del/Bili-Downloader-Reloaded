package me.heartalborada.biliDownloader.Utils.Okhttp;

import lombok.Getter;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


public class SimpleCookieJar implements CookieJar {
    @Getter
    private final HashMap<String, List<Cookie>> cookieStore;

    public SimpleCookieJar() {
        super();
        cookieStore = new HashMap<>();
    }

    public SimpleCookieJar(HashMap<String, List<Cookie>> CookieData) {
        super();
        cookieStore = CookieData;
    }

    private static String getUrlKey(HttpUrl httpUrl) {
        String[] arr = httpUrl.host().split("\\.");
        return String.format("%s:%d", arr[arr.length - 2] + "." + arr[arr.length - 1], httpUrl.port());
    }

    @Override
    public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
        String url = getUrlKey(httpUrl);
        if (cookieStore.containsKey(url)) {
            cookieStore.put(url, list);
        } else {
            List<Cookie> l = cookieStore.get(url) != null ? cookieStore.get(url) : new LinkedList<>();
            for (Cookie c : list) {
                boolean isMatch = false;
                for (Cookie c1 : l) {
                    if (Objects.equals(c1.domain(), c.domain()) && c1.expiresAt() < c.expiresAt()) {
                        l.remove(c1);
                        l.add(c);
                        isMatch = true;
                        break;
                    }
                }
                if (!isMatch)
                    l.add(c);
            }
            cookieStore.remove(url);
            cookieStore.put(url, l);
        }
    }

    @NotNull
    @Override
    public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
        String url = getUrlKey(httpUrl);
        List<Cookie> m = cookieStore.get(url);
        if (m != null)
            return cookieStore.get(url);
        return new LinkedList<>();
    }
}
