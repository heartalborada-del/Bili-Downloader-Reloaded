package me.heartalborada.biliDownloader.Cli;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import me.heartalborada.biliDownloader.Bili.Beans.LoginData;
import me.heartalborada.biliDownloader.Bili.Beans.QRLogin.QRLoginToken;
import me.heartalborada.biliDownloader.Bili.Beans.Video.Sub.Honor;
import me.heartalborada.biliDownloader.Bili.Beans.Video.Sub.Pages;
import me.heartalborada.biliDownloader.Bili.Beans.Video.Sub.Staff;
import me.heartalborada.biliDownloader.Bili.Beans.Video.VideoData;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.Audio;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.Sub.Video;
import me.heartalborada.biliDownloader.Bili.Beans.VideoStream.VideoStreamData;
import me.heartalborada.biliDownloader.Bili.BiliInstance;
import me.heartalborada.biliDownloader.Bili.Exceptions.BadRequestDataException;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Cli.Terminal.TerminalProcessProgress;
import me.heartalborada.biliDownloader.Cli.Terminal.TerminalSelection;
import me.heartalborada.biliDownloader.FFmpeg.Convertor;
import me.heartalborada.biliDownloader.FFmpeg.Locator;
import me.heartalborada.biliDownloader.Interfaces.EncoderProgressListenerM;
import me.heartalborada.biliDownloader.Interfaces.SelectionCallback;
import me.heartalborada.biliDownloader.Main;
import me.heartalborada.biliDownloader.MultiThreadDownload.DownloadInstance;
import me.heartalborada.biliDownloader.MultiThreadDownload.MultiThreadDownloader;
import me.heartalborada.biliDownloader.Utils.NoWhiteQRCode;
import org.jline.jansi.Ansi;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.*;
import picocli.CommandLine;
import ws.schild.jave.info.MultimediaInfo;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.heartalborada.biliDownloader.Utils.Utils.NumberUtils.amountConversion;
import static me.heartalborada.biliDownloader.Utils.Utils.*;

@CommandLine.Command(name = "",
        description = {"Features"},
        subcommands = {Commands.FFMpeg.class, Commands.Bilibili.class, CommandLine.HelpCommand.class}
)
@SuppressWarnings("Duplicates")
public class Commands implements Runnable {
    private final Terminal terminal;
    private final String Version = "1.0";
    Commands(Terminal terminal) {
        this.terminal = terminal;
    }

    private static void DisplayResizeAndUpdate(List<AttributedString> list, Display display, Terminal terminal) {
        int maxLen = 20;
        for (AttributedString as : list) {
            if (maxLen < as.toString().length() * 2) maxLen = as.toString().length() * 2;
        }
        display.resize(list.size(), terminal.getWidth() == 0 ? maxLen : terminal.getWidth());
        display.update(list, terminal.getSize().cursorPos(list.size(), 0));
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
            version = Version,
            subcommands = {CommandLine.HelpCommand.class}

    )
    class Bilibili {
        private final Pattern avMatch = Pattern.compile("^((av)[0-9]+)", Pattern.CASE_INSENSITIVE);
        private final Pattern bvMatch = Pattern.compile("^((bv)[0-9a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        BiliInstance biliInstance = Main.getBiliInstance();

        public Bilibili() {
        }

        @CommandLine.Command(
                name = "login",
                mixinStandardHelpOptions = true,
                subcommands = {CommandLine.HelpCommand.class},
                description = "Log in to your Bilibili account"
        )
        void login(
                @CommandLine.Option(names = {"-t", "--type"}, description = "Allow \"QR\"", required = true) String type
        ) throws IOException, WriterException {
            if (!(type != null && type.equalsIgnoreCase("qr"))) {
                AttributedString string = new AttributedStringBuilder()
                        .style(new AttributedStyle().background(AttributedStyle.RED).foreground(AttributedStyle.BLACK))
                        .append("Unknown Type: ")
                        .append(type)
                        .toAttributedString();
                Display display = new Display(terminal, false);
                DisplayResizeAndUpdate(new ArrayList<>() {{ add(string); }},display,terminal);
                return;
            }
            QRLoginToken token = biliInstance.getLogin().getQR().getQRLoginToken();
            {
                ArrayList<AttributedString> QRDisplay = new ArrayList<>();
                HashMap<EncodeHintType, Serializable> hints = new HashMap<>();
                hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
                hints.put(EncodeHintType.MARGIN, 0);
                BitMatrix bitMatrix = new NoWhiteQRCode().encode(
                        token.getQRUrl(),
                        BarcodeFormat.QR_CODE,
                        0,
                        0,
                        hints
                );
                for (int j = 0; j < bitMatrix.getHeight(); j++) {
                    AttributedStringBuilder asb = new AttributedStringBuilder();
                    for (int i = 0; i < bitMatrix.getWidth(); i++) {
                        if (bitMatrix.get(i, j)) {
                            asb.style(new AttributedStyle().background(AttributedStyle.WHITE)).append("  ");
                        } else {
                            asb.style(new AttributedStyle().background(AttributedStyle.BLACK)).append("  ");
                        }
                    }
                    QRDisplay.add(asb.toAttributedString());
                }
                Display display = new Display(terminal, false);
                DisplayResizeAndUpdate(QRDisplay,display,terminal);
            }
            Display display = new Display(terminal, false);
            LinkedList<AttributedString> originalList = new LinkedList<>() {{
                AttributedString as = new AttributedStringBuilder()
                        .style(new AttributedStyle().background(255, 248, 220).foreground(AttributedStyle.BLACK))
                        .append("URL: ")
                        .style(new AttributedStyle().background(205, 133, 63))
                        .append(token.getQRUrl()).toAttributedString();
                display.resize(2, as.length() + 1);
                add(as);
            }};
            Thread mainThread = Thread.currentThread();
            boolean[] flag = new boolean[]{false};
            ScheduledFuture<?> future = biliInstance.getLogin().getQR().loginWithQrLogin(token, new Callback() {
                @Override
                public void onSuccess(LoginData data, String message, int code) {
                    Main.getDataManager().getData().getBilibili().setCookies(data.getCookies());
                    Main.getDataManager().getData().getBilibili().setRefreshToken(data.getRefreshToken());
                    Main.getDataManager().getData().getBilibili().setLatestRefreshTimestamp(data.getTimestamp());
                    try {
                        Main.getDataManager().save();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    LinkedList<AttributedString> modify = new LinkedList<>() {{
                        addAll(originalList);
                        AttributedStringBuilder asb = new AttributedStringBuilder()
                                .style(new AttributedStyle().background(AttributedStyle.GREEN))
                                .append("SUCCESS");
                        add(asb.toAttributedString());
                    }};
                    display.update(modify, terminal.getSize().cursorPos(2, 0));
                    flag[0] = true;
                    mainThread.interrupt();
                }

                @Override
                public void onFailure(Exception e, String cause, int code) {
                    LinkedList<AttributedString> modify = new LinkedList<>() {{
                        addAll(originalList);
                        AttributedStringBuilder asb = new AttributedStringBuilder()
                                .style(new AttributedStyle().background(AttributedStyle.RED))
                                .append("Failed")
                                .append(" - ")
                                .append(cause);
                        add(asb.toAttributedString());
                    }};
                    display.update(modify, terminal.getSize().cursorPos(2, 0));
                    mainThread.interrupt();
                }

                @Override
                public void onUpdate(String message, int code) {
                    LinkedList<AttributedString> modify = new LinkedList<>() {{
                        addAll(originalList);
                        AttributedStringBuilder asb = new AttributedStringBuilder()
                                .style(new AttributedStyle().background(AttributedStyle.YELLOW))
                                .append(String.format("[Waiting Response - %s]", message))
                                .append(String.format("[Remaining time - %d]", (token.getRegTimestamp() + 180 * 1000 - System.currentTimeMillis()) / 1000));
                        add(asb.toAttributedString());
                    }};
                    display.update(modify, terminal.getSize().cursorPos(2, 0));
                }
            });
            while (!(future.isDone() || future.isCancelled())) {
                try {
                    int ignore = terminal.reader().read();
                } catch (InterruptedIOException ignore) {
                    if (!future.isCancelled()) {
                        future.cancel(true);
                    }
                    if (!flag[0]) {
                        LinkedList<AttributedString> modify = new LinkedList<>() {{
                            addAll(originalList);
                            AttributedStringBuilder asb = new AttributedStringBuilder()
                                    .style(new AttributedStyle().background(AttributedStyle.RED))
                                    .append("Canceled");
                            add(asb.toAttributedString());
                        }};
                        display.update(modify, terminal.getSize().cursorPos(2, 0));
                    }
                    break;
                }
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
        ) throws IOException, IllegalStateException {
            Display display = new Display(terminal, false);
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
                    LinkedList<AttributedString> list = new LinkedList<>();
                    list.add(new AttributedStringBuilder()
                            .style(new AttributedStyle().foreground(AttributedStyle.RED))
                            .append(String.format("UnknownID: %s", id)).toAttributedString());
                    DisplayResizeAndUpdate(list,display,terminal);
                    return;
                }
            } catch (BadRequestDataException exception) {
                LinkedList<AttributedString> list = new LinkedList<>();
                list.add(
                        new AttributedStringBuilder().style(new AttributedStyle().foreground(AttributedStyle.RED)).append(String.format("Error: [%d]%s", exception.getCode(), exception.getMessage())).toAttributedString()
                );
                DisplayResizeAndUpdate(list, display, terminal);
                return;
            }
            LinkedList<AttributedString> list = new LinkedList<>();
            list.add(new AttributedStringBuilder().append(String.format("■ Title: %s", videoData.getTitle())).toAttributedString());
            list.add(new AttributedStringBuilder().append(String.format("■ BVID: %s | AID: %d", videoData.getBvid(), videoData.getAid())).toAttributedString());
            list.add(new AttributedStringBuilder().append(String.format("■ View: %s | Like: %s | Coin: %s | Favorite: %s", amountConversion(BigDecimal.valueOf(videoData.getStat().getView())), amountConversion(BigDecimal.valueOf(videoData.getStat().getLike())), amountConversion(BigDecimal.valueOf(videoData.getStat().getCoin())), amountConversion(BigDecimal.valueOf(videoData.getStat().getFavorite())))).toAttributedString());
            list.add(new AttributedStringBuilder().append(String.format("■ Publish Time: %s", zonedDateToFormatString(timestampToDate(videoData.getPublishDate() * 1000L, ZoneId.of("+08:00")), "yyyy-MM-dd HH:mm:ss v"))).toAttributedString());
            if (videoData.getStaff() == null || videoData.getStaff().isEmpty()) {
                list.add(new AttributedStringBuilder().append(String.format("■ Owner: %s", videoData.getOwner().getName())).toAttributedString());
            } else {
                list.add(new AttributedStringBuilder().append("■ Staffs: ").toAttributedString());
                for (Staff obj : videoData.getStaff()) {
                    list.add(new AttributedStringBuilder().append(String.format("  □ %s - %s", obj.getTitle(), obj.getName())).toAttributedString());
                }
            }
            if (!(videoData.getHonorReply().getHonor() == null)) {
                list.add(new AttributedStringBuilder().append("■ Honors: ").toAttributedString());
                for (Honor obj : videoData.getHonorReply().getHonor()) {
                    String emojiPrefix = "□";
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
                    list.add(new AttributedStringBuilder().append(String.format("  %s %s%n", emojiPrefix, obj.getDesc())).toAttributedString());
                }
            }
            DisplayResizeAndUpdate(list, display, terminal);
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
        ) throws IOException {
            Display display = new Display(terminal, false);
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
                    LinkedList<AttributedString> list = new LinkedList<>();
                    list.add(new AttributedStringBuilder()
                            .style(new AttributedStyle().foreground(AttributedStyle.RED))
                            .append(String.format("UnknownID: %s", id)).toAttributedString());
                    DisplayResizeAndUpdate(list,display,terminal);
                    return;
                }
            } catch (BadRequestDataException exception) {
                LinkedList<AttributedString> list = new LinkedList<>();
                list.add(
                        new AttributedStringBuilder().style(new AttributedStyle().foreground(AttributedStyle.RED)).append(String.format("Error: [%d]%s", exception.getCode(), exception.getMessage())).toAttributedString()
                );
                DisplayResizeAndUpdate(list, display, terminal);
                return;
            }
            final VideoStreamData streamData = new VideoStreamData();
            final Pages pageData = new Pages();
            if (videoData.getPages().size() == 1) {
                pageData.setAll(videoData.getPages().get(0));
                streamData.setAll(biliInstance.getVideo().getVideoStreamData(videoData, videoData.getPages().get(0).getCid()));
            } else {
                LinkedHashMap<String, SelectionCallback<String>> selectionCallbackMap = new LinkedHashMap<>();
                for (Pages p : videoData.getPages()) {
                    selectionCallbackMap.put(p.getPart(), TargetObj -> {
                        try {
                            pageData.setAll(p);
                            VideoStreamData v = biliInstance.getVideo().getVideoStreamData(videoData, p.getCid());
                            streamData.setAll(v);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                TerminalSelection<String> ts = new TerminalSelection<>(terminal, selectionCallbackMap, null);
                ts.start();
                while (!(ts.isDone() || ts.isCancelled())) {
                    if (ts.isCancelled()) return;
                }
                if (ts.isCancelled()) return;
            }
            final Video video = new Video();
            LinkedHashMap<String, SelectionCallback<String>> selectionCallbackMap = new LinkedHashMap<>();
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
                        break;
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
                selectionCallbackMap.put(String.format("%s - %s", qn, codec), TargetObj -> video.setAll(obj));
            }
            TerminalSelection<String> ts = new TerminalSelection<>(terminal, selectionCallbackMap, null);
            ts.start();
            while (!(ts.isDone() || ts.isCancelled())) {
                if (ts.isCancelled()) return;
            }
            if (ts.isCancelled()) return;
            final Audio audio = new Audio();
            if (needAudioQuality) {
                selectionCallbackMap.clear();
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
                    selectionCallbackMap.put(String.format("%s", qn), TargetObj -> audio.setAll(obj));
                }
                ts = new TerminalSelection<>(terminal, selectionCallbackMap, null);
                ts.start();
                while (!(ts.isDone() || ts.isCancelled())) {
                    if (ts.isCancelled()) return;
                }
            } else {
                int m = 0;
                for (Audio obj : streamData.getDash().getAudio()) {
                    if (obj.getId() > m) {
                        audio.setAll(obj);
                        m = audio.getId();
                    }
                }
            }
            MultiThreadDownloader downloader = new MultiThreadDownloader(biliInstance.getClient());
            download(downloader, new URL(video.getBaseUrl()), new File(Main.getCachePath(), String.format("%d/%d-%d-%d.m4s", videoData.getAid(), pageData.getCid(), pageData.getPage(), video.getId())));
            download(downloader, new URL(audio.getBaseUrl()), new File(Main.getCachePath(), String.format("%d/%d-%d-%d.m4s", videoData.getAid(), pageData.getCid(), pageData.getPage(), audio.getId())));
            try {
                convert(
                        new File(Main.getCachePath(), String.format("%d/%d-%d-%d.m4s", videoData.getAid(), pageData.getCid(), pageData.getPage(), video.getId())),
                        new File(Main.getCachePath(), String.format("%d/%d-%d-%d.m4s", videoData.getAid(), pageData.getCid(), pageData.getPage(), audio.getId())),
                        new File(Main.getDownloadPath(), String.format("%d/%d_p%d.mp4", videoData.getAid(), pageData.getCid(), pageData.getPage() - 1))
                );
            } catch (IOException e) {
                terminal.writer().println("\33[1;31mFFmpeg not install, skip convert!\33[0m");
            }
        }

        private void convert(File v, File a, File o) throws IOException {
            final boolean[] flag = new boolean[]{false};
            final TerminalProcessProgress progress = new TerminalProcessProgress(terminal);
            progress.setTotalSize(100);
            final Thread m = Thread.currentThread();
            Future<?> f = Convertor.doConvertor(
                    v, a, o,
                    new EncoderProgressListenerM() {
                        @Override
                        public void onFailed(Throwable throwable) {
                            //TODO output log
                            flag[0] = true;
                            progress.setFailed();
                            progress.close();
                            m.interrupt();
                        }

                        @Override
                        public void sourceInfo(MultimediaInfo info) {
                        }

                        @Override
                        public void progress(int i) {
                            progress.update(i);
                            if (i == -1 || i == 100) {
                                progress.update(100);
                                progress.close();
                                flag[0] = true;
                                m.interrupt();
                            }
                        }

                        @Override
                        public void message(String s) {
                        }
                    }
            );
            Attributes attr = terminal.enterRawMode();
            terminal.puts(InfoCmp.Capability.keypad_xmit, new Object());
            terminal.flush();
            while (!progress.isClosed() || !f.isCancelled()) {
                try {
                    int ignore = terminal.reader().read();
                } catch (InterruptedIOException ignore) {
                    if (!f.isCancelled()) {
                        f.cancel(true);
                    }
                    if (!flag[0]) {
                        Display display = new Display(terminal, false);
                        LinkedList<AttributedString> modify = new LinkedList<>() {{
                            AttributedStringBuilder asb = new AttributedStringBuilder()
                                    .style(new AttributedStyle().background(AttributedStyle.RED))
                                    .append("Canceled");
                            add(asb.toAttributedString());
                        }};
                        display.update(modify, terminal.getSize().cursorPos(1, 0));
                    }
                    break;
                }
            }
            terminal.puts(InfoCmp.Capability.keypad_local, new Object());
            if(attr != null) terminal.setAttributes(attr);
            terminal.flush();
        }

        private void download(MultiThreadDownloader downloader, URL url, File filePath) throws IOException {
            final Thread thread = Thread.currentThread();
            final TerminalProcessProgress progress = new TerminalProcessProgress(terminal, "{stat} | {bar} | {percentage}% | {processed}/{total}");
            DownloadInstance instance = downloader.download(url, filePath, new MultiThreadDownloader.Callback() {
                long size = 0;

                @Override
                public void onSuccess(long fileSize) {
                    progress.update(fileSize);
                    progress.close();
                    thread.interrupt();
                }

                @Override
                public void onFailure(Exception e, String cause) {
                    progress.setFailed();
                    progress.close();
                    thread.interrupt();
                }

                @Override
                public void onStart(long fileSize) {
                    progress.setTotalSize(fileSize);
                }

                @Override
                public void newSpeedStat(long speed, boolean isStop) {
                    size += speed;
                    progress.update(size);
                }
            });
            Attributes attr = terminal.enterRawMode();
            terminal.puts(InfoCmp.Capability.keypad_xmit, new Object());
            terminal.flush();
            while (!(instance.isDone() || instance.isFailed())) {
                try {
                    int ignore = terminal.reader().read();
                } catch (InterruptedIOException ignore) {
                    if (!(instance.isFailed() || instance.isDone())) {
                        instance.stop();
                    }
                    break;
                }
            }
            terminal.puts(InfoCmp.Capability.keypad_local, new Object());
            if(attr != null) terminal.setAttributes(attr);
            terminal.flush();
        }
    }

    @CommandLine.Command(
            name = "ffmpeg",
            aliases = {"ffm"},
            description = {"FFMpeg Features"},
            mixinStandardHelpOptions = true,
            version = Version,
            subcommands = {CommandLine.HelpCommand.class}
    )
    class FFMpeg {
        @CommandLine.Command(
                name = "check",
                mixinStandardHelpOptions = true,
                subcommands = {CommandLine.HelpCommand.class},
                description = "Check FFMpeg is installed"
        )
        void check() {
            Display disp = new Display(terminal,false);
            try {
                String path = new Locator(Main.getConfigManager().getConfig().getFFMpegPath()).getExecutablePath();
                DisplayResizeAndUpdate(new ArrayList<>(){{
                    add(new AttributedStringBuilder()
                            .style(new AttributedStyle().background(72,209,204).foreground(AttributedStyle.BLACK))
                            .append("Found FFMpeg, Absolute Path: ")
                            .style(new AttributedStyle().background(230,197,65).foreground(AttributedStyle.BLACK))
                            .append(path)
                            .append(new Ansi().reset().toString())
                            .toAttributedString());
                }},disp,terminal);
            }catch (IOException ignore) {
                DisplayResizeAndUpdate(new ArrayList<>(){{
                    add(new AttributedStringBuilder()
                            .style(new AttributedStyle().background(AttributedStyle.RED).foreground(AttributedStyle.WHITE))
                            .append("Cannot found FFMpeg. Are you installed it?")
                            .append(new Ansi().reset().toString())
                            .toAttributedString());
                }},disp,terminal);
            }
        }
    }
}