package me.heartalborada.biliDownloader.Cli;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import me.heartalborada.biliDownloader.Bili.Beans.loginData;
import me.heartalborada.biliDownloader.Bili.BiliInstance;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Cli.Terminal.Progress;
import me.heartalborada.biliDownloader.Main;
import me.heartalborada.biliDownloader.Utils.NotWriteQRCode;
import org.jline.console.CommandInput;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.newFixedThreadPool;

@CommandLine.Command(name = "",
        description = {"Bilibili Features"},
        subcommands = {Commands.Bilibili.class,CommandLine.HelpCommand.class}
)
public class Commands implements Runnable{
    static LineReaderImpl reader;
    PrintWriter out;


    static Terminal terminal;
    public void setReader(LineReader reader){
        this.reader = (LineReaderImpl) reader;
        out = terminal.writer();
    }
    Commands(Terminal terminal) {
        this.terminal = terminal;
    }
    @Override
    public void run() {
        out.println(new CommandLine(this).getUsageMessage());
    }

    static class TPool {
        private final ExecutorService service;
        TPool() {
            service = newFixedThreadPool(3);
        }
        TPool(int size) {
            service = newFixedThreadPool(size);
        }
        Future<?> newTask(Runnable task) {
            return service.submit(task);
        }
    }
    @CommandLine.Command(
            name = "bilibili",
            mixinStandardHelpOptions = true,
            version = "0.1",
            subcommands = {CommandLine.HelpCommand.class}
    )
    static class Bilibili implements Runnable {
        static BiliInstance biliInstance;

        static {
            try {
                biliInstance = new BiliInstance();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        @CommandLine.Command(
                name = "login",
                mixinStandardHelpOptions = false,
                subcommands = {CommandLine.HelpCommand.class},
                description = "Log in to your Bilibili account"
        )
        void login(@CommandLine.Option(names = {"-t", "--type"}, description = "Allow \"QR\"") String type) throws InterruptedException {
            PrintStream out = new PrintStream(CliMain.getTerminal().output());
            if(type.equalsIgnoreCase("qr")) {
                CliMain.getTerminal().pause(true);
                Timer task = biliInstance.new Login().new QR().loginWithQrLogin(new Callback() {
                    final Long curT = System.currentTimeMillis();
                    final Progress progress = new Progress(out, 20, 0, 180);
                    private Future<?> f;

                    @SuppressWarnings("all")
                    @Override
                    public void onSuccess(loginData data, String message, int code) {
                        Main.getDataManager().getData().getBilibili().setCookies(data.getCookies());
                        Main.getDataManager().getData().getBilibili().setRefreshToken(data.getRefreshToken());
                        Main.getDataManager().getData().getBilibili().setLatestRefreshTimestamp(data.getTimestamp());
                        progress.UpgradeProgress(
                                (System.currentTimeMillis() - curT) / 1000,
                                "\33[48;5;2m[SUCCESS]\33[0m",
                                String.format("\33[48;5;2m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m", code, "SUCCESS")
                        );
                        f.cancel(true);
                        CliMain.getTerminal().resume();
                    }

                    @Override
                    public void onFailure(Exception e, String cause, int code) {
                        progress.UpgradeProgress(
                                (System.currentTimeMillis() - curT) / 1000,
                                "\33[48;5;1m[FAILED]\33[0m",
                                String.format("\33[48;5;1m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m", code, cause, (System.currentTimeMillis() - curT) / 1000)
                        );
                        f.cancel(true);
                        CliMain.getTerminal().resume();
                    }

                    @Override
                    public void onUpdate(String message, int code) {
                        progress.UpgradeProgress(
                                (System.currentTimeMillis() - curT) / 1000,
                                "\33[48;5;3m[WAITING]\33[0m",
                                String.format("\33[48;5;3m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m", code, message, (System.currentTimeMillis() - curT) / 1000)
                        );
                        try {
                            CliMain.getTerminal().pause(true);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onGetQRUrl(String QRUrl) {
                        out.printf("%n");
                        try {
                            HashMap<EncodeHintType, Serializable> hints = new HashMap<EncodeHintType, Serializable>();
                            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
                            hints.put(EncodeHintType.MARGIN, 0);
                            BitMatrix bitMatrix = new NotWriteQRCode().encode(
                                    QRUrl,
                                    BarcodeFormat.QR_CODE,
                                    1,
                                    1,
                                    hints);
                            for (int j = 0; j < bitMatrix.getHeight(); j++) {
                                for (int i = 0; i < bitMatrix.getWidth(); i++) {
                                    if (bitMatrix.get(i, j)) {
                                        out.print("\33[48;5;7m  ");
                                    } else {
                                        out.print("\33[0m  ");
                                    }
                                }
                                out.println("\33[0m");
                            }
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                        out.printf("%n\33[48;5;4mURL:\33[48;5;5m%s\33[0m%n", QRUrl);
                    }
                });
                while (true) {
                    Scanner scanner = new Scanner(CliMain.getTerminal().input());
                    try {
                        scanner.nextLine();
                    } catch (NoSuchElementException | IllegalStateException ignore) {
                        out.println("\n\33[48;5;1m[CANCELED]\33[0m");
                        out.close();
                        CliMain.getTerminal().resume();
                        task.cancel();
                        task.purge();
                        return;
                    }
                }
            } else {
                out.printf("\33[1;31mUnknown Type: %s\33[0m%n",type);
            }
        }
        @Override
        public void run() {
            CliMain.getTerminal().writer().println(new CommandLine(this).getUsageMessage());
        }
    }
}
