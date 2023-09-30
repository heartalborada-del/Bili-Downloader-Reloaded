module me.heartalborada.biliDownloader {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.google.gson;
    requires okhttp3;
    requires java.xml;
    requires java.logging;
    requires kotlin.stdlib;
    requires org.jsoup;
    requires org.jetbrains.annotations;
    requires lombok;

    opens me.heartalborada.biliDownloader.Bili.beans to com.google.gson;
    opens me.heartalborada.biliDownloader.utils.managers.beans to com.google.gson;
    opens me.heartalborada.biliDownloader to javafx.fxml;
    exports me.heartalborada.biliDownloader;
    exports me.heartalborada.biliDownloader.UI;
    opens me.heartalborada.biliDownloader.UI to javafx.fxml;
    exports me.heartalborada.biliDownloader.Cli;
    opens me.heartalborada.biliDownloader.Cli to javafx.fxml;
}