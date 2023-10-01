package me.heartalborada.biliDownloader.Utils.Managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import me.heartalborada.biliDownloader.Utils.Managers.Beans.configInstance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class configManager {
    @Getter
    @Setter
    private static configInstance config;
    private final File location;
    public configManager(File location) throws IOException {
        this.location = location;
        if(location.exists()) {
            String unFormat = Files.readString(location.toPath());
            config = new Gson().fromJson(unFormat, configInstance.class);
        } else {
            config = new configInstance();
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
