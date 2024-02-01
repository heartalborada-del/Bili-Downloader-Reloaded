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
import me.heartalborada.biliDownloader.Cli.Terminal.TerminalSelection;
import me.heartalborada.biliDownloader.Interfaces.SelectionCallback;
import me.heartalborada.biliDownloader.Main;
import me.heartalborada.biliDownloader.Utils.NoWhiteQRCode;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.heartalborada.biliDownloader.Utils.Utils.NumberUtils.amountConversion;
import static me.heartalborada.biliDownloader.Utils.Utils.*;

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

    private static void DisplayResizeAndUpdate(List<AttributedString> list, Display display, Terminal terminal) {
        int maxLen = 20;
        for (AttributedString as : list) {
            if (maxLen < calculateHalfWidth(as.toString()) + 1) maxLen = calculateHalfWidth(as.toString()) + 1;
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
            version = "0.1",
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
                display.resize(1, terminal.getWidth() == 0 ? 20 : terminal.getWidth());
                display.update(new ArrayList<>() {{
                    add(string);
                }}, terminal.getSize().cursorPos(1, 0));
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
                display.resize(QRDisplay.size(), terminal.getWidth() == 0 ? QRDisplay.get(1).length() + 1 : terminal.getWidth());
                display.update(QRDisplay, terminal.getSize().cursorPos(41, 0));
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
                    display.resize(list.size(), terminal.getWidth()-1);
                    display.update(list, terminal.getSize().cursorPos(1, 0));
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
                for (Staff obj : videoData.getStaff()) {list.add(new AttributedStringBuilder().append(String.format("  □ %s - %s", obj.getTitle(), obj.getName())).toAttributedString());}
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
                    display.resize(list.size(), terminal.getWidth()-1);
                    display.update(list, terminal.getSize().cursorPos(1, 0));
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
                            streamData.setAll(biliInstance.getVideo().getVideoStreamData(videoData, p.getCid()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                TerminalSelection<String> ts = new TerminalSelection<>(terminal, selectionCallbackMap, null);
                ts.start();
                while (!(ts.isDone() || ts.isCancelled())) {
                    if(ts.isCancelled()) return;
                }
                if(ts.isCancelled()) return;
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
                selectionCallbackMap.put(String.format("%s - %s",qn,codec),TargetObj -> {
                    video.setAll(obj);
                });
            }
            TerminalSelection<String> ts = new TerminalSelection<>(terminal, selectionCallbackMap, null);
            ts.start();
            while (!(ts.isDone() || ts.isCancelled())) {
                if(ts.isCancelled()) return;
            }
            if(ts.isCancelled()) return;
            final Audio audio = new Audio();
            if(needAudioQuality) {
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
                    selectionCallbackMap.put(String.format("%s",qn),TargetObj -> {
                        audio.setAll(obj);
                    });
                }
                ts = new TerminalSelection<>(terminal, selectionCallbackMap, null);
                ts.start();
                while (!(ts.isDone() || ts.isCancelled())) {
                    if(ts.isCancelled()) return;
                }
                if(ts.isCancelled()) return;
            } else {
                int m = 0;
                for (Audio obj : streamData.getDash().getAudio()) {
                    if (obj.getId() > m) {
                        audio.setAll(obj);
                        m = audio.getId();
                    }
                }
            }
        }

        @CommandLine.Command(
                name = "test"
        )
        void test() throws InterruptedException, IOException {
            Display display = new Display(terminal, false);
            ArrayList<AttributedString> QRDisplay = new ArrayList<>();
            {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.style(new AttributedStyle().background(AttributedStyle.BLUE)).append("A");
                QRDisplay.add(asb.toAttributedString());
            }
            {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.style(new AttributedStyle().background(AttributedStyle.RED)).append("B");
                QRDisplay.add(asb.toAttributedString());
            }
            display.resize(QRDisplay.size(), terminal.getWidth() == 0 ? 82 : terminal.getWidth());
            display.update(QRDisplay, terminal.getSize().cursorPos(QRDisplay.size(), 0));
        }
    }
}