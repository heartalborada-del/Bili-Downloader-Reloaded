package me.heartalborada.biliDownloader.Cli.Terminal;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;

public class Utils {
    public static int getStringPrintLen(@NotNull String value) {
        try {
            return value.getBytes("Unicode").length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
