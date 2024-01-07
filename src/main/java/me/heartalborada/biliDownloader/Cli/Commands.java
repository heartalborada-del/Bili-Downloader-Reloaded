package me.heartalborada.biliDownloader.Cli;

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
import me.heartalborada.biliDownloader.Bili.Exceptions.BadRequestDataException;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Cli.Terminal.TerminalProcessProgress;
import me.heartalborada.biliDownloader.Main;
import me.heartalborada.biliDownloader.MultiThreadDownload.DownloadInstance;
import me.heartalborada.biliDownloader.MultiThreadDownload.MultiThreadDownloader;
import me.heartalborada.biliDownloader.Utils.NotWriteQRCode;
import me.heartalborada.biliDownloader.Utils.Utils;
import okhttp3.Cookie;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import picocli.CommandLine;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.heartalborada.biliDownloader.Utils.Utils.NumberUtils.amountConversion;
import static me.heartalborada.biliDownloader.Utils.Utils.timestampToDate;
import static me.heartalborada.biliDownloader.Utils.Utils.zonedDateToFormatString;

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
                    final TerminalProcessProgress progress = new TerminalProcessProgress(lineReader);
                    {
                        progress.setTotalSize(180);
                    }
                    @SuppressWarnings("all")
                    @Override
                    public void onSuccess(LoginData data, String message, int code) {
                        Main.getDataManager().getData().getBilibili().setCookies(data.getCookies());
                        Main.getDataManager().getData().getBilibili().setRefreshToken(data.getRefreshToken());
                        Main.getDataManager().getData().getBilibili().setLatestRefreshTimestamp(data.getTimestamp());
                        progress.update(180);
                        progress.updateText("\33[48;5;2m[SUCCESS]\33[0m");
                        progress.close();
                        terminal.resume();
                    }

                    @Override
                    public void onFailure(Exception e, String cause, int code) {
                        progress.updateText("\33[48;5;1m[FAILED]\33[0m");
                        progress.setFailed();
                        progress.close();
                        terminal.resume();
                    }

                    @Override
                    public void onUpdate(String message, int code) {
                        progress.update((System.currentTimeMillis() - curT) / 1000);
                        progress.updateText(String.format("\33[48;5;3m[WAITING]\33[0m\33[48;5;3m[%d: %s]\33[0m\33[48;5;4m[Time: %d-180]\33[0m", code, message, (System.currentTimeMillis() - curT) / 1000));
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
            try {
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
            } catch (BadRequestDataException exception) {
                terminal.writer().printf("\33[31mError: [%d]%s\33[0m%n", exception.getCode(), exception.getMessage());
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
            VideoData videoData = null;
            try {
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
            } catch (BadRequestDataException exception) {
                terminal.writer().printf("\33[31mError: [%d]%s\33[0m%n", exception.getCode(), exception.getMessage());
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
            final TerminalProcessProgress AudioProgressBar = new TerminalProcessProgress(lineReader);
            final TerminalProcessProgress VideoProgressBar = new TerminalProcessProgress(lineReader);
            instances.add(downloader.download(
                    new URL(video.getBaseUrl()),
                    new File(Main.getCachePath(), String.format("%d/%d.mp4", videoData.getAid(), cid)),
                    new MultiThreadDownloader.Callback() {
                        long total = 0;
                        @Override
                        public void onSuccess(long fileSize) {
                            VideoProgressBar.update(fileSize);
                            VideoProgressBar.close();
                            VideoProgressBar.rerender();
                        }

                        @Override
                        public void onFailure(Exception e, String cause) {
                            VideoProgressBar.setFailed();
                            VideoProgressBar.close();
                            VideoProgressBar.rerender();
                            e.printStackTrace();
                        }

                        @Override
                        public void onStart(long fileSize) {
                            VideoProgressBar.setTotalSize(fileSize);
                            VideoProgressBar.rerender();
                            //System.out.println(fileSize);
                        }

                        @Override
                        public void newSpeedStat(long speed, boolean isStop) {
                            if (isStop) return;
                            total += speed;
                            VideoProgressBar.update(total);
                            VideoProgressBar.updateText(String.format("Speed: %s/s", Utils.byteToUnit(speed)));
                            VideoProgressBar.rerender();
                        }
                    },
                    header
            ));
            if (audio != null) {
                instances.add(downloader.download(
                        new URL(audio.getBaseUrl()),
                        new File(Main.getCachePath(), String.format("%d/%d.m4a", videoData.getAid(), cid)),
                        new MultiThreadDownloader.Callback() {
                            long total = 0;

                            @Override
                            public void onSuccess(long fileSize) {
                                AudioProgressBar.update(fileSize);
                                AudioProgressBar.close();
                            }

                            @Override
                            public void onFailure(Exception e, String cause) {
                                AudioProgressBar.close();
                                e.printStackTrace();
                            }

                            @Override
                            public void onStart(long fileSize) {
                                AudioProgressBar.setTotalSize(fileSize);
                            }

                            @Override
                            public void newSpeedStat(long speed, boolean isStop) {
                                if(isStop) return;
                                total += speed;
                                AudioProgressBar.update(total);
                                AudioProgressBar.updateText(String.format("Speed: %s/s", Utils.byteToUnit(speed)));
                            }
                        },
                        header
                ));
            }
            NonBlockingReader reader = terminal.reader();
            Thread thread = new Thread(()-> {
                while (true) {
                    boolean flag = true;
                    for (DownloadInstance i : instances) {
                        if (!i.isDone()) {
                            flag = false;
                        }
                    }
                    if (flag) {
                        terminal.resume();
                        reader.shutdown();
                        return;
                    }
                }
            });
            thread.start();
            while (true) {
                try {
                    reader.read();
                    boolean flag = true;
                    for (DownloadInstance i : instances) {
                        if (!i.isDone()) {
                            flag = false;
                        }
                    }
                    if (flag) {
                        AudioProgressBar.close();
                        VideoProgressBar.close();
                        terminal.resume();
                        reader.shutdown();
                        thread.stop();
                        return;
                    }
                } catch (InterruptedIOException eof) {
                    AudioProgressBar.close();
                    VideoProgressBar.close();
                    for (DownloadInstance i : instances) {
                        i.stop();
                    }
                    terminal.writer().printf("\33[48;5;1m[CANCELED]\33[0m%n");
                    reader.shutdown();
                    thread.stop();
                    terminal.resume();
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @CommandLine.Command(
                name = "test"
        )
        void test() throws InterruptedException {
            TerminalProcessProgress progress = new TerminalProcessProgress(lineReader);
            progress.setTotalSize(114514);
            progress.update(1919);
            progress.updateText("WDNMD");
            Thread.sleep(100);
            progress.setFailed();
        }
    }
}