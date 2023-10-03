package me.heartalborada.biliDownloader.Cli.Terminal;

import java.io.PrintStream;

import static me.heartalborada.biliDownloader.Cli.Terminal.Utils.getStringPrintLen;

public abstract class Terminal {
    private String lastString = "";
    private final String format;
    private final PrintStream printStream;

    public Terminal(String format, PrintStream printStream) {
        this.format = format;
        this.printStream = printStream;
    }

    public void UpgradeString(Object... a) {
        StringBuilder newStr = new StringBuilder(String.format(format, a));
        for(int i=lastString.length();i>0;i--) {
            printStream.print("\b");
        }
        String cp = newStr.toString();
        newStr.append(" ".repeat(Math.max(0, getStringPrintLen(lastString) - getStringPrintLen(cp) + 1)));
        lastString = cp;
        printStream.print(newStr);
    }
}
