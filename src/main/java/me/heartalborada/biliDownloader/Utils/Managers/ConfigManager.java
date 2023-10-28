package me.heartalborada.biliDownloader.Utils.Managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import me.heartalborada.biliDownloader.Utils.Managers.Beans.ConfigInstance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ConfigManager {
    @Getter
    @Setter
    private ConfigInstance config;
    private final File location;
    public ConfigManager(File location) throws IOException {
        this.location = location;
        if(location.exists()) {
            String unFormat = Files.readString(location.toPath());
            config = new Gson().fromJson(unFormat, ConfigInstance.class);
        } else {
            config = new ConfigInstance();
            save();
        }
    }

    public void save() throws IOException {
        String format = new GsonBuilder().setPrettyPrinting().create().toJson(config);
        if(location.exists() && !location.delete())
            throw new IOException(String.format("Failed delete file: %s",location.getPath()));
        Files.copy(new ByteArrayInputStream(format.getBytes(StandardCharsets.UTF_8)), location.toPath());
    }
}
