package me.heartalborada.biliDownloader.Cli.Commands.SubCommands;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.QRCode;
import me.heartalborada.biliDownloader.Bili.Beans.loginData;
import me.heartalborada.biliDownloader.Bili.BiliInstance;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Cli.Commands.Utils.Terminal.Progress;
import me.heartalborada.biliDownloader.Main;
import me.heartalborada.biliDownloader.Utils.NotWriteQRCode;
import org.jline.builtins.Options;
import org.jline.console.impl.SystemRegistryImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;

import static me.heartalborada.biliDownloader.Utils.Util.StrArrToSting;

public class BilibiliSubCommands {
    public static void login(org.jline.terminal.Terminal terminal, PrintStream out, InputStream in, String[] argv) throws Options.HelpException, IOException, SystemRegistryImpl.UnknownCommandException {
        String[] usage = {
                "login - login your Bilibili account",
                "Usage: login [OPTIONS]",
                "  -? --help                     Displays command help",
                "  qr                            Login by QRCode"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new Options.HelpException(opt.usage());
        }
        if(Objects.equals(opt.args().get(0), "qr")) {
            terminal.pause();
            qrLogin(terminal,out,in);
        } else {
            throw new SystemRegistryImpl.UnknownCommandException(String.format("Unknown command: %s %s","login",StrArrToSting(argv)));
        }
    }

    public static void qrLogin(org.jline.terminal.Terminal terminal, PrintStream out, InputStream in) throws IOException {
        terminal.pause();
        BiliInstance biliInstance = new BiliInstance();
        biliInstance.new Login().new QR().loginWithQrLogin(new Callback() {
            final Progress progress = new Progress(out,20,0,180);
            int count = 0;
            @SuppressWarnings("all")
            @Override
            public void onSuccess(loginData data, String message, int code) {
                Main.getDataManager().getData().getBilibili().setCookies(data.getCookies());
                Main.getDataManager().getData().getBilibili().setRefreshToken(data.getRefreshToken());
                Main.getDataManager().getData().getBilibili().setLatestRefreshTimestamp(data.getTimestamp());
                progress.UpgradeProgress(180,"\33[48;5;2m[SUCCESS]\33[0m",String.format("\33[48;5;2m[%d: %s]\33[0m",code,"SUCCESS"));
                terminal.resume();
            }

            @Override
            public void onFailure(Exception e, String cause, int code) {
                progress.UpgradeProgress(180,"\33[48;5;1m[FAILED]\33[0m",String.format("\33[48;5;1m[%d: %s]\33[0m",code,cause));
                terminal.resume();
            }

            @Override
            public void onUpdate(String message, int code) {
                count++;
                progress.UpgradeProgress(count,"\33[48;5;3m[WAITING]\33[0m",String.format("\33[48;5;3m[%d: %s]\33[0m",code,message));
            }

            @Override
            public void onGetQRUrl(String QRUrl) {
                out.printf("%n");
                try {
                    HashMap<EncodeHintType, Serializable> hints = new HashMap<EncodeHintType, java.io.Serializable>();
                    hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
                    hints.put(EncodeHintType.MARGIN,0);
                    new QRCode();
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
                out.printf("%n\33[48;5;4mURL:\33[48;5;5m%s\33[0m%n",QRUrl);
            }
        });
    }
}
