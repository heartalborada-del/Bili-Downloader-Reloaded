package me.heartalborada.biliDownloader.Cli.Terminal;

import java.io.PrintStream;

import static me.heartalborada.biliDownloader.Cli.Terminal.Utils.getStringPrintLen;

public abstract class Terminal {
    private final String format;
    private final PrintStream printStream;
    private String lastString = "";

    public Terminal(String format, PrintStream printStream) {
        this.format = format;
        this.printStream = printStream;
    }

    public void UpgradeString(Object... a) {
        StringBuilder newStr = new StringBuilder(String.format(format, a));
        printStream.printf("\33[%dD",lastString.length());
        lastString = newStr.toString();
        printStream.print(newStr);
    }
}
