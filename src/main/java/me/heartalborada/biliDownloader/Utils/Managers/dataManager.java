package me.heartalborada.biliDownloader.Utils.Managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import me.heartalborada.biliDownloader.Utils.Managers.Beans.dataInstance;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class dataManager {
    @Getter
    private static dataInstance data;
    private final File location;
    public dataManager(File location) throws IOException {
        this.location = location;
        if(location.exists()) {
            String unFormat = Files.readString(location.toPath());
            data = new Gson().fromJson(unFormat, dataInstance.class);
        } else {
            data = new dataInstance();
            save();
        }
    }

    public void save() throws IOException {
        String format = new GsonBuilder().setPrettyPrinting().create().toJson(data);
        if(location.exists() && !location.delete())
            throw new IOException(String.format("Failed delete file: %s",location.getPath()));
        Files.copy(new ByteArrayInputStream(format.getBytes(StandardCharsets.UTF_8)), location.toPath());
    }
}
