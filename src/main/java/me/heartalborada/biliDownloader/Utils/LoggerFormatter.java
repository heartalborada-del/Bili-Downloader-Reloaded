package me.heartalborada.biliDownloader.Utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = "\n" + sw;
        }
        Thread currentThread = Thread.currentThread();

        return String.format("[%s] [%s][%s] %s%s\n",
                record.getLevel().toString(),
                Thread.currentThread().getName(),
                record.getLoggerName(),
                message,
                throwable);
    }

    public static Logger installFormatter(Logger logger){
        if(null != logger){
            logger.setUseParentHandlers(false);
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new LoggerFormatter());
            logger.addHandler(consoleHandler);
        }
        return logger;
    }
}
