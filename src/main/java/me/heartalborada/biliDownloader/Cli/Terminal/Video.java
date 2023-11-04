package me.heartalborada.biliDownloader.Cli.Terminal;

import java.io.PrintStream;

public class Video extends Terminal {
    private final int terminalWidth;

    public Video(String format, PrintStream printStream, int terminalWidth) {
        super(format, printStream);
        this.terminalWidth = terminalWidth >= 20 ? terminalWidth : 80;
    }
}
