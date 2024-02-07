package me.heartalborada.biliDownloader.FFmpeg;

import ws.schild.jave.process.ProcessLocator;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

public class Locator implements ProcessLocator {
    private String findFFMPEG(String UserProvideFFMPEGPath) throws IOException {
        //Check System is installed FFMPEG
        {
            ProcessBuilder processBuilder = new ProcessBuilder(new LinkedList<>(){{
                add("ffmpeg");
                add("-version");
            }});
            try {
                Process proc = processBuilder.start();
                if (proc.waitFor() == 0) {
                    return "ffmpeg";
                }
            } catch (IOException | IllegalThreadStateException | InterruptedException ignore) {}
        }
        //Check User Provide FFMPEG
        {
            String os = System.getProperty("os.name").toLowerCase();
            String suffix = os.contains("windows") ? "exe" : "";
            ProcessBuilder processBuilder = new ProcessBuilder(new LinkedList<>(){{
                add(new File(UserProvideFFMPEGPath, suffix.isEmpty() ? "ffmpeg" :"ffmpeg.exe").getPath());
                add("-version");
            }});
            try {
                Process proc = processBuilder.start();
                if (proc.waitFor() == 0) {
                    return new File(UserProvideFFMPEGPath, suffix.isEmpty() ? "ffmpeg" :"ffmpeg.exe").getPath();
                }
            } catch (IOException | IllegalThreadStateException | InterruptedException ignore) {}
        }
        throw new IOException("Couldn't find FFMPEG, ARE YOU INSTALLED IT?");
    }
    private final String path;
    public Locator(String UserProvideFFMPEGPath) throws IOException {
        path = findFFMPEG(UserProvideFFMPEGPath);
    }
    @Override
    public String getExecutablePath() {
        return path;
    }
}
