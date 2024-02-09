package me.heartalborada.biliDownloader.FFmpeg;

import ws.schild.jave.process.ProcessLocator;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

public class Locator implements ProcessLocator {
    private final String path;

    public Locator(String UserProvideFFMPEGPath) throws IOException {
        path = findFFMPEG(UserProvideFFMPEGPath);
    }

    private String findFFMPEG(String UserProvideFFMPEGPath) throws IOException {
        String suffix = System.getProperty("os.name").toLowerCase().contains("windows") ? "exe" : "";
        //Check System is installed FFMPEG
        {
            ProcessBuilder processBuilder = new ProcessBuilder(new LinkedList<>() {{
                add("ffmpeg");
                add("-version");
            }});
            try {
                Process proc = processBuilder.start();
                if (proc.waitFor() == 0) {
                    String pathSeparator = System.getProperty("os.name").startsWith("Windows") ? ";" : ":";
                    String[] paths = System.getenv("PATH").split(pathSeparator);
                    for (String p : paths) {
                        File f = new File(p, suffix.isEmpty() ? "ffmpeg" : "ffmpeg.exe");
                        if (f.exists()) {
                            return f.getPath();
                        }
                    }
                }
            } catch (IOException | IllegalThreadStateException | InterruptedException ignore) {
            }
        }
        //Check User Provide FFMPEG
        {
            ProcessBuilder processBuilder = new ProcessBuilder(new LinkedList<>() {{
                add(new File(UserProvideFFMPEGPath, suffix.isEmpty() ? "ffmpeg" : "ffmpeg.exe").getPath());
                add("-version");
            }});
            try {
                Process proc = processBuilder.start();
                if (proc.waitFor() == 0) {
                    return new File(UserProvideFFMPEGPath, suffix.isEmpty() ? "ffmpeg" : "ffmpeg.exe").getPath();
                }
            } catch (IOException | IllegalThreadStateException | InterruptedException ignore) {
            }
        }
        throw new IOException("Couldn't find FFMPEG, ARE YOU INSTALLED IT?");
    }

    @Override
    public String getExecutablePath() {
        return path;
    }
}
