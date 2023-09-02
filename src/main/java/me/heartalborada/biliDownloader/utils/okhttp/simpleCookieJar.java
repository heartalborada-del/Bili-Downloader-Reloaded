package me.heartalborada.biliDownloader.utils.okhttp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.Getter;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class simpleCookieJar implements CookieJar {
    @Getter
    private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();
    @Override
    public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
        if(cookieStore.containsKey(httpUrl)) {
            cookieStore.put(httpUrl, list);
        } else {
            List<Cookie> l = cookieStore.get(httpUrl);
            for (Cookie c: list) {
                if(!l.contains(c)) {
                    l.add(c);
                } else {
                    l.remove(c);
                    l.add(c);
                }
            }
        }
    }

    @NotNull
    @Override
    public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
        return cookieStore.get(httpUrl);
    }

    public JsonElement dumpCookies(){
       Gson g = new Gson();
       return JsonParser.parseString(g.toJson(cookieStore));
    }
}
