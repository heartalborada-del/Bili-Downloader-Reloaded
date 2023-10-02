package me.heartalborada.biliDownloader.Cli.Commands.Utils.Terminal;

import java.io.PrintStream;

public class Progress extends Terminal{
    private final double length,min,max;

    public Progress(PrintStream printStream,int length,int min,int max) {
        super("%s[%s]%s", printStream);
        this.length = length;
        this.min = min;
        this.max = max;
    }

    public void UpgradeProgress(double current) {
        UpgradeProgress(current,"","");
    }

    public void UpgradeProgress(double current,String front,String end){
        StringBuilder ab = new StringBuilder();
        if(current>max)
            current = max;
        else if (current<min)
            current = min;
        double v = (max - min) / length;
        for (double i = min; i < max; i+=v) {
            if(i<current) {
                ab.append("=");
            } else if (i == current || (i-v<current && i+v>current)){
                ab.append(">");
            } else {
                ab.append("-");
            }
        }
        UpgradeString(front,ab.toString(),end);
    }
}
