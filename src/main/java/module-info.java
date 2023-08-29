module me.heartalborada.biliDownloader {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.google.gson;
    requires okhttp3;
    requires static lombok;
    requires annotations;
    requires java.xml;
    requires java.logging;

    opens me.heartalborada.biliDownloader.Bili.bean to com.google.gson;
    opens me.heartalborada.biliDownloader to javafx.fxml;
    exports me.heartalborada.biliDownloader;
    exports me.heartalborada.biliDownloader.UI;
    opens me.heartalborada.biliDownloader.UI to javafx.fxml;
}