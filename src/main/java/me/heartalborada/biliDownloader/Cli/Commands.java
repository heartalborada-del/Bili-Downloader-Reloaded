package me.heartalborada.biliDownloader.Cli;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import me.heartalborada.biliDownloader.Bili.Beans.LoginData;
import me.heartalborada.biliDownloader.Bili.Beans.Video.Sub.Honor;
import me.heartalborada.biliDownloader.Bili.Beans.Video.Sub.Staff;
import me.heartalborada.biliDownloader.Bili.Beans.Video.VideoData;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.Audio;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.Video;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.VideoStreamData;
import me.heartalborada.biliDownloader.Bili.BiliInstance;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Cli.Terminal.Progress;
import me.heartalborada.biliDownloader.Main;
import me.heartalborada.biliDownloader.MultiThreadDownload.DownloadInstance;
import me.heartalborada.biliDownloader.MultiThreadDownload.MultiThreadDownloader;
import me.heartalborada.biliDownloader.Utils.NotWriteQRCode;
import me.heartalborada.biliDownloader.Utils.Util;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import okhttp3.Cookie;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.heartalborada.biliDownloader.Utils.Util.NumberUtils.amountConversion;
import static me.heartalborada.biliDownloader.Utils.Util.timestampToDate;
import static me.heartalborada.biliDownloader.Utils.Util.zonedDateToFormatString;

@CommandLine.Command(name = "",
        description = {"Bilibili Features"},
        subcommands = {Commands.Bilibili.class, CommandLine.HelpCommand.class}
)
@SuppressWarnings("Duplicates")
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
        BiliInstance biliInstance = Main.getBiliInstance();

        public Bilibili() {
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
                                String.format("\33[48;5;2m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m%\33[48;5;3m[Press Enter To Conutinue]\33[0m", code, "SUCCESS", (System.currentTimeMillis() - curT) / 1000)
                        );
                        terminal.resume();
                    }

                    @Override
                    public void onFailure(Exception e, String cause, int code) {
                        progress.UpgradeProgress(
                                (System.currentTimeMillis() - curT) / 1000,
                                "\33[48;5;1m[FAILED]\33[0m",
                                String.format("\33[48;5;1m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m%n\33[48;5;3m[Press Enter To Conutinue]\33[0m", code, cause, (System.currentTimeMillis() - curT) / 1000)
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

                new Thread(() -> {
                    while (true) {
                        try {
                            lineReader.readLine();
                            if (!terminal.paused()) {
                                task.cancel();
                                task.purge();
                                terminal.resume();
                                return;
                            }
                        } catch (UserInterruptException | EndOfFileException ignore) {
                            if (terminal.paused()) {
                                task.cancel();
                                task.purge();
                                out.printf("\33[48;5;1m[CANCELED]\33[0m%n");
                                terminal.resume();
                            }
                            return;
                        }
                    }
                }).start();
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
            terminal.writer().printf("■ Title: %s%n", videoData.getTitle());
            terminal.writer().printf("■ BVID: %s | AVID: %d%n", videoData.getBvid(), videoData.getAid());
            terminal.writer().printf("■ View: %s | Like: %s | Coin: %s | Favorite: %s%n",
                    amountConversion(BigDecimal.valueOf(videoData.getStat().getView())),
                    amountConversion(BigDecimal.valueOf(videoData.getStat().getLike())),
                    amountConversion(BigDecimal.valueOf(videoData.getStat().getCoin())),
                    amountConversion(BigDecimal.valueOf(videoData.getStat().getFavorite())));
            terminal.writer().printf("■ Publish Time: %s%n", zonedDateToFormatString(
                    timestampToDate(videoData.getPublishDate() * 1000L, ZoneId.of("+08:00")
                    ), "yyyy-MM-dd HH:mm:ss v"));
            if (videoData.getStaff() == null || videoData.getStaff().isEmpty()) {
                terminal.writer().printf("■ Owner: %s%n", videoData.getOwner().getName());
            } else {
                terminal.writer().println("■ Staffs: ");
                for (Staff obj : videoData.getStaff()) {
                    terminal.writer().printf("  □ %s - %s%n", obj.getTitle(), obj.getName());
                }
            }
            if (!(videoData.getHonorReply().getHonor() == null)) {
                terminal.writer().println("■ Honor: ");
                for (Honor obj : videoData.getHonorReply().getHonor()) {
                    String emojiPrefix = "";
                    if (!disEmoji) {
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
                    terminal.writer().printf("  □ %s%s%n", emojiPrefix, obj.getDesc());
                }
            }
            terminal.resume();
        }

        @CommandLine.Command(
                name = "download",
                mixinStandardHelpOptions = true,
                subcommands = {CommandLine.HelpCommand.class},
                description = "Download Bilibili video"
        )
        void download(
                @CommandLine.Option(names = {"-i", "--id"}, description = "Bilibili Video ID (BVID/AID)", required = true) String id,
                @CommandLine.Option(names = {"-au", "--audio"}, description = "Is Need choose download Audio Quality") boolean needAudioQuality
        ) throws IOException, InterruptedException {
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
            long cid;
            VideoStreamData streamData;
            if (videoData.getPages().size() == 1) {
                streamData = biliInstance.getVideo().getVideoStreamData(videoData, 0);
                cid = videoData.getCid();
            } else {
                terminal.resume();
                while (true) {
                    try {
                        String line = lineReader.readLine(String.format("\33[48;5;3m[Ctrl+C to quit]\33[0m Input a number [0-%d]: ", videoData.getPages().size() - 1));
                        if (!Objects.equals(line, "")) {
                            try {
                                int parse = Integer.parseInt(line);
                                if (parse >= videoData.getPages().size() || parse < 0) {
                                    terminal.writer().printf("\33[1;31m%s\33[0m%n", "Invalid Input");
                                    continue;
                                }
                                streamData = biliInstance.getVideo().getVideoStreamData(videoData, parse);
                                cid = videoData.getPages().get(parse).getCid();
                                terminal.pause(true);
                                break;
                            } catch (NumberFormatException e) {
                                terminal.writer().printf("\33[1;31m%s\33[0m%n", "Invalid Input");
                            }
                        }
                    } catch (UserInterruptException | EndOfFileException ignore) {
                        terminal.writer().printf("\33[48;5;1m[CANCELED]\33[0m%n");
                        terminal.resume();
                        return;
                    }
                }
                terminal.pause(true);
            }
            LinkedList<Video> videoList = new LinkedList<>();
            terminal.writer().println("■ Video Quality & encode: ");
            for (Video obj : streamData.getDash().getVideo()) {
                String codec = "Unknown", qn = "Unknown";
                switch (obj.getCodecid()) {
                    case 7:
                        codec = "AVC";
                        break;
                    case 12:
                        codec = "H.264";
                        break;
                    case 13:
                        codec = "H.265";
                }
                switch (obj.getId()) {
                    case 6:
                        qn = "240P";
                        break;
                    case 16:
                        qn = "360P";
                        break;
                    case 32:
                        qn = "480P";
                        break;
                    case 64:
                        qn = "720P";
                        break;
                    case 74:
                        qn = "720P60";
                        break;
                    case 80:
                        qn = "1080P";
                        break;
                    case 112:
                        qn = "1080P+";
                        break;
                    case 116:
                        qn = "1080P60";
                        break;
                    case 120:
                        qn = "4K";
                        break;
                    case 125:
                        qn = "HDR";
                        break;
                    case 126:
                        qn = "Dolby";
                        break;
                    case 127:
                        qn = "8K";
                        break;
                }
                videoList.add(obj);
                terminal.writer().printf("  %d. %s - %s%n", videoList.size(), qn, codec);
            }
            Video video;
            while (true) {
                terminal.resume();
                try {
                    String line = lineReader.readLine(String.format("\33[48;5;3m[Ctrl+C to quit]\33[0m Select Video Quality [1-%d]: ", videoList.size()));
                    if (!Objects.equals(line, "")) {
                        try {
                            int parse = Integer.parseInt(line);
                            if (parse > videoList.size() || parse <= 0) {
                                terminal.writer().printf("\33[1;31m%s\33[0m%n", "Invalid Input");
                                continue;
                            }
                            terminal.pause(true);
                            video = videoList.get(parse - 1);
                            break;
                        } catch (NumberFormatException e) {
                            terminal.writer().printf("\33[1;31m%s\33[0m%n", "Invalid Input");
                        }
                    }
                } catch (UserInterruptException | EndOfFileException ignore) {
                    terminal.writer().printf("\33[48;5;1m[CANCELED]\33[0m%n");
                    terminal.resume();
                    return;
                }
                terminal.pause(false);
            }
            Audio audio = null;
            if (needAudioQuality) {
                terminal.writer().println("■ Video Quality: ");
                LinkedList<Audio> audioList = new LinkedList<>();
                for (Audio obj : streamData.getDash().getAudio()) {
                    String qn = "Unknown";
                    switch (obj.getId()) {
                        case 30216:
                            qn = "64K";
                            break;
                        case 30232:
                            qn = "132K";
                            break;
                        case 30280:
                            qn = "192K";
                            break;
                        case 30250:
                            qn = "Dolby";
                            break;
                        case 30251:
                            qn = "Hi-Res";
                    }
                    audioList.add(obj);
                    terminal.writer().printf("  %d. %s%n", audioList.size(), qn);
                }
                while (true) {
                    try {
                        String line = lineReader.readLine(String.format("\33[48;5;3m[Ctrl+C to quit]\33[0m Select Audio Quality [i-%d]: ", videoList.size()));
                        if (!Objects.equals(line, "")) {
                            try {
                                int parse = Integer.parseInt(line);
                                if (parse > audioList.size() || parse <= 0) {
                                    terminal.writer().printf("\33[1;31m%s\33[0m%n", "Invalid Input");
                                    continue;
                                }
                                terminal.pause(true);
                                audio = audioList.get(parse - 1);
                                break;
                            } catch (NumberFormatException e) {
                                terminal.writer().printf("\33[1;31m%s\33[0m%n", "Invalid Input");
                            }
                        }
                    } catch (UserInterruptException | EndOfFileException ignore) {
                        terminal.writer().printf("\33[48;5;1m[CANCELED]\33[0m%n");
                        terminal.resume();
                        return;
                    }
                }
            } else {
                int m = 0;
                for (Audio obj : streamData.getDash().getAudio()) {
                    if (obj.getId() > m) {
                        audio = obj;
                        m = audio.getId();
                    }
                }
            }
            MultiThreadDownloader downloader = new MultiThreadDownloader();
            LinkedList<DownloadInstance> instances = new LinkedList<>();
            LinkedHashMap<String, String> header = new LinkedHashMap<>();
            StringBuilder cookieStr = new StringBuilder();
            for (Cookie cookie : Main.getDataManager().getData().getBilibili().getCookies().get("bilibili.com:443")) {
                cookieStr.append(String.format("%s=%s;", cookie.name(), cookie.value()));
            }
            header.put("Cookie", cookieStr.toString());
            terminal.pause();
            NativeKeyListener listener = new NativeKeyListener() {
                @Override
                public void nativeKeyTyped(NativeKeyEvent e) {
                    if (e.getKeyCode() == NativeKeyEvent.VC_C && e.getModifiers() == NativeKeyEvent.CTRL_L_MASK) {
                        for (DownloadInstance instance : instances) {
                            instance.stop();
                        }
                        terminal.writer().printf("\33[48;5;1m[CANCELED]\33[0m%n");
                        terminal.resume();
                    }
                }

                @Override
                public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
                    NativeKeyListener.super.nativeKeyPressed(nativeEvent);
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
                    NativeKeyListener.super.nativeKeyReleased(nativeEvent);
                }
            };

            instances.add(downloader.download(
                    new URL(video.getBaseUrl()),
                    new File(Main.getCachePath(), String.format("%d/%d.mp4", videoData.getAid(), cid)),
                    new MultiThreadDownloader.Callback() {
                        final ProgressBar VideoprogressBar;
                        long total = 0;

                        {
                            ProgressBarBuilder builder = new ProgressBarBuilder();
                            builder.setStyle(ProgressBarStyle.ASCII);
                            //builder.clearDisplayOnFinish();
                            builder.setTaskName("Video");
                            builder.setInitialMax(0);
                            VideoprogressBar = builder.build();
                        }

                        @Override
                        public void onSuccess(long fileSize) {
                            VideoprogressBar.stepTo(fileSize);
                            VideoprogressBar.close();
                        }

                        @Override
                        public void onFailure(Exception e, String cause) {
                            VideoprogressBar.close();
                            e.printStackTrace();
                        }

                        @Override
                        public void onStart(long fileSize) {
                            VideoprogressBar.maxHint(fileSize);
                        }

                        @Override
                        public void newSpeedStat(long speed) {
                            total += speed;
                            VideoprogressBar.stepTo(total);
                            VideoprogressBar.setExtraMessage(String.format("Speed: %s/s", Util.byteToUnit(speed)));
                        }
                    },
                    header
            ));
            if (audio != null) {
                instances.add(downloader.download(
                        new URL(audio.getBaseUrl()),
                        new File(Main.getCachePath(), String.format("%d/%d.m4a", videoData.getAid(), cid)),
                        new MultiThreadDownloader.Callback() {
                            final ProgressBar AudioProgressBar;
                            long total = 0;

                            {
                                ProgressBarBuilder builder = new ProgressBarBuilder();
                                builder.setStyle(ProgressBarStyle.ASCII);
                                //builder.clearDisplayOnFinish();
                                builder.setTaskName("Audio");
                                builder.setInitialMax(0);
                                AudioProgressBar = builder.build();
                            }

                            @Override
                            public void onSuccess(long fileSize) {
                                AudioProgressBar.stepTo(fileSize);
                                AudioProgressBar.close();
                            }

                            @Override
                            public void onFailure(Exception e, String cause) {
                                AudioProgressBar.close();
                                e.printStackTrace();
                            }

                            @Override
                            public void onStart(long fileSize) {
                                AudioProgressBar.maxHint(fileSize);
                            }

                            @Override
                            public void newSpeedStat(long speed) {
                                total += speed;
                                AudioProgressBar.stepTo(total);
                                AudioProgressBar.setExtraMessage(String.format("Speed: %s/s", Util.byteToUnit(speed)));
                            }
                        },
                        header
                ));
            }
            GlobalScreen.addNativeKeyListener(listener);
            while (true) {
                boolean flag = true;
                for (DownloadInstance i : instances) {
                    if (!i.isDone()) {
                        flag = false;
                    }
                }
                if (flag) {
                    terminal.resume();
                    GlobalScreen.removeNativeKeyListener(listener);
                    return;
                }
            }
        }
    }
}