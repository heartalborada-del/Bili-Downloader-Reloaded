module me.heartalborada.biliDownloader {
    requires javafx.controls;
    requires javafx.fxml;

    requires me.tongfei.progressbar;
    requires java.sql;
    requires com.google.gson;
    requires okhttp3;
    requires java.xml;
    requires java.logging;
    requires kotlin.stdlib;
    requires org.jsoup;
    requires org.jetbrains.annotations;
    requires static lombok;
    requires org.jline;
    requires com.google.zxing;
    requires info.picocli;
    requires picocli.shell.jline3;

    opens me.heartalborada.biliDownloader.Bili.Beans to com.google.gson;
    opens me.heartalborada.biliDownloader.Utils.Managers.Beans to com.google.gson;
    opens me.heartalborada.biliDownloader to javafx.fxml;
    exports me.heartalborada.biliDownloader;
    exports me.heartalborada.biliDownloader.UI;
    opens me.heartalborada.biliDownloader.UI to javafx.fxml;
    exports me.heartalborada.biliDownloader.Cli;
    opens me.heartalborada.biliDownloader.Cli to info.picocli;
    opens me.heartalborada.biliDownloader.Bili.Beans.Video to com.google.gson;
    opens me.heartalborada.biliDownloader.Bili.Beans.Video.Sub to com.google.gson;
    opens me.heartalborada.biliDownloader.Bili.Beans.VideoStream to com.google.gson;
    opens me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub to com.google.gson;
}