package me.heartalborada.biliDownloader.Cli;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import me.heartalborada.biliDownloader.Bili.Beans.LoginData;
import me.heartalborada.biliDownloader.Bili.Beans.Video.Sub.Honor;
import me.heartalborada.biliDownloader.Bili.Beans.Video.Sub.Staff;
import me.heartalborada.biliDownloader.Bili.Beans.Video.VideoData;
import me.heartalborada.biliDownloader.Bili.BiliInstance;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Cli.Terminal.Progress;
import me.heartalborada.biliDownloader.Main;
import me.heartalborada.biliDownloader.Utils.NotWriteQRCode;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.heartalborada.biliDownloader.Utils.Util.NumberUtils.amountConversion;
import static me.heartalborada.biliDownloader.Utils.Util.timestampToDate;
import static me.heartalborada.biliDownloader.Utils.Util.zonedDateToFormatString;

@CommandLine.Command(name = "",
        description = {"Bilibili Features"},
        subcommands = {Commands.Bilibili.class, CommandLine.HelpCommand.class}
)
public class Commands implements Runnable {
    private final Terminal terminal;
    private final LineReader lineReader;

    Commands(Terminal terminal, LineReader lineReader) {
        this.terminal = terminal;
        this.lineReader = lineReader;
    }

    @Override
    public void run() {
        terminal.writer().println(new CommandLine(this).getUsageMessage());
    }

    @CommandLine.Command(
            name = "bilibili",
            aliases = {"bili"},
            description = {"Bilibili Features"},
            mixinStandardHelpOptions = true,
            version = "0.1",
            subcommands = {CommandLine.HelpCommand.class}

    )
    class Bilibili {
        private final Pattern avMatch = Pattern.compile("^((av|)[0-9]+)", Pattern.CASE_INSENSITIVE);
        private final Pattern bvMatch = Pattern.compile("^((bv|)[0-9a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        BiliInstance biliInstance = new BiliInstance();

        public Bilibili() throws IOException {
        }

        @CommandLine.Command(
                name = "login",
                mixinStandardHelpOptions = true,
                subcommands = {CommandLine.HelpCommand.class},
                description = "Log in to your Bilibili account"
        )
        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
        void login(
                @CommandLine.Option(names = {"-t", "--type"}, description = "Allow \"QR\"", required = true) String type
        ) throws InterruptedException {
            PrintStream out = new PrintStream(CliMain.getTerminal().output());
            if (type != null && type.equalsIgnoreCase("qr")) {
                CliMain.getTerminal().pause(true);
                Timer task = biliInstance.getLogin().getQR().loginWithQrLogin(new Callback() {
                    final Long curT = System.currentTimeMillis();
                    final Progress progress = new Progress(out, 20, 0, 180);

                    @SuppressWarnings("all")
                    @Override
                    public void onSuccess(LoginData data, String message, int code) {
                        Main.getDataManager().getData().getBilibili().setCookies(data.getCookies());
                        Main.getDataManager().getData().getBilibili().setRefreshToken(data.getRefreshToken());
                        Main.getDataManager().getData().getBilibili().setLatestRefreshTimestamp(data.getTimestamp());
                        progress.UpgradeProgress(
                                (System.currentTimeMillis() - curT) / 1000,
                                "\33[48;5;2m[SUCCESS]\33[0m",
                                String.format("\33[48;5;2m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m", code, "SUCCESS", (System.currentTimeMillis() - curT) / 1000)
                        );
                        terminal.resume();
                    }

                    @Override
                    public void onFailure(Exception e, String cause, int code) {
                        progress.UpgradeProgress(
                                (System.currentTimeMillis() - curT) / 1000,
                                "\33[48;5;1m[FAILED]\33[0m",
                                String.format("\33[48;5;1m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m", code, cause, (System.currentTimeMillis() - curT) / 1000)
                        );
                        terminal.resume();
                    }

                    @Override
                    public void onUpdate(String message, int code) {
                        progress.UpgradeProgress(
                                (System.currentTimeMillis() - curT) / 1000,
                                "\33[48;5;3m[WAITING]\33[0m",
                                String.format("\33[48;5;3m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m", code, message, (System.currentTimeMillis() - curT) / 1000)
                        );
                        try {
                            terminal.pause(true);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onGetQRUrl(String QRUrl) {
                        out.printf("%n");
                        try {
                            HashMap<EncodeHintType, Serializable> hints = new HashMap<>();
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
                    try {
                        lineReader.readLine();
                        if (!terminal.paused()) {
                            task.cancel();
                            task.purge();
                            terminal.resume();
                            return;
                        }
                    } catch (UserInterruptException ignore) {
                        if (terminal.paused()) {
                            task.cancel();
                            task.purge();
                            out.printf("\33[48;5;1m[CANCELED]\33[0m%n");
                            terminal.resume();
                        }
                        return;
                    }
                }
            } else {
                out.printf("\33[1;31mUnknown Type: %s\33[0m%n", type);
            }
        }

        @CommandLine.Command(
                name = "info",
                mixinStandardHelpOptions = true,
                subcommands = {CommandLine.HelpCommand.class},
                description = "Get current Bilibili video info"
        )
        void info(
                @CommandLine.Option(names = {"-i", "--id"}, description = "Bilibili Video ID (BVID/AID)", required = true) String id,
                @CommandLine.Option(names = {"--no-emoji"}, description = "Disable Emoji Prefix") boolean disEmoji
        ) throws IOException, InterruptedException, IllegalStateException {
            terminal.pause(true);
            VideoData videoData;
            if (Pattern.matches(avMatch.pattern(), id.toLowerCase())) {
                Matcher matcher = avMatch.matcher(id);
                String bid = "  ";
                while (matcher.find())
                    bid = matcher.group();
                videoData = biliInstance.getVideo().getVideoData(
                        Integer.decode(bid.substring(2))
                );
            } else if (Pattern.matches(bvMatch.pattern(), id.toLowerCase())) {
                Matcher matcher = bvMatch.matcher(id);
                String bid = "  ";
                while (matcher.find())
                    bid = matcher.group();
                videoData = biliInstance.getVideo().getVideoData(
                        bid.substring(2)
                );
            } else {
                terminal.writer().printf("\33[31mUnknowID: %s\33[0m%n", id);
                terminal.resume();
                return;
            }
            terminal.writer().printf("\u25A0 Title: %s%n", videoData.getTitle());
            terminal.writer().printf("\u25A0 BVID: %s | AVID: %d%n", videoData.getBvid(), videoData.getAid());
            terminal.writer().printf("\u25A0 View: %s | Like: %s | Coin: %s | Favorite: %s%n",
                    amountConversion(BigDecimal.valueOf(videoData.getStat().getView())),
                    amountConversion(BigDecimal.valueOf(videoData.getStat().getLike())),
                    amountConversion(BigDecimal.valueOf(videoData.getStat().getCoin())),
                    amountConversion(BigDecimal.valueOf(videoData.getStat().getFavorite())));
            terminal.writer().printf("\u25A0 Publish Time: %s%n", zonedDateToFormatString(
                    timestampToDate(videoData.getPublishDate() * 1000L, ZoneId.of("+08:00")
                    ), "yyyy-MM-dd HH:mm:ss v"));
            if (videoData.getStaff() == null || videoData.getStaff().isEmpty()) {
                terminal.writer().printf("\u25A0 Owner: %s%n", videoData.getOwner().getName());
            } else {
                terminal.writer().println("\u25A0 Staffs: ");
                for (Staff obj : videoData.getStaff()) {
                    terminal.writer().printf("  \u25A1 %s - %s%n", obj.getTitle(), obj.getName());
                }
            }
            if(!(videoData.getHonorReply().getHonor() == null)) {
                terminal.writer().println("\u25A0 Honor: ");
                for (Honor obj: videoData.getHonorReply().getHonor()) {
                    String emojiPrefix = "";
                    if(!disEmoji) {
                        switch (obj.getType()) {
                            case 1:
                                emojiPrefix = "\uD83D\uDDC3 - ";
                                break;
                            case 2:
                                emojiPrefix = "\uD83C\uDF1F - ";
                                break;
                            case 3:
                                emojiPrefix = "\uD83D\uDD1D - ";
                                break;
                            case 4:
                                emojiPrefix = "\uD83D\uDD25 - ";
                                break;
                        }
                    }
                    terminal.writer().printf("  \u25A1 %s%s%n", emojiPrefix, obj.getDesc());
                }
            }
            //terminal.writer().println(new Gson().toJson(videoData));
            terminal.resume();
        }
    }
}