package me.heartalborada.biliDownloader.Cli.Commands.Utils.Terminal;

import java.io.PrintStream;

public abstract class Terminal {
    private int latestLength;
    private final String format;
    private final PrintStream printStream;

    public Terminal(String format, PrintStream printStream) {
        this.format = format;
        this.printStream = printStream;
        latestLength = 0;
    }

    public void UpgradeString(Object... a) {
        StringBuilder newStr = new StringBuilder(String.format(format, a));
        for(int i=latestLength;i>0;i--) {
            printStream.print("\b");
        }
        int len = newStr.length();
        while (latestLength>newStr.length()) {
            newStr.append(" ");
        }
        latestLength = len;
        printStream.print(newStr);
    }
}
