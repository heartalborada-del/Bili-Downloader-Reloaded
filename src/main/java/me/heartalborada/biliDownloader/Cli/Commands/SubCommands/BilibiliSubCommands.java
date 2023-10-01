package me.heartalborada.biliDownloader.Cli.Commands.SubCommands;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import me.heartalborada.biliDownloader.Bili.Beans.loginData;
import me.heartalborada.biliDownloader.Bili.Interfaces.Callback;
import me.heartalborada.biliDownloader.Bili.biliInstance;
import me.heartalborada.biliDownloader.Utils.Managers.dataManager;
import org.jline.builtins.Commands;
import org.jline.builtins.Options;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import static me.heartalborada.biliDownloader.Utils.util.StrArrToSting;

public class BilibiliSubCommands {
    public static void login(Terminal terminal, PrintStream out, String[] argv) throws Options.HelpException, IOException, SystemRegistryImpl.UnknownCommandException {
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
            testQrLogin(terminal,out);
        } else {
            throw new SystemRegistryImpl.UnknownCommandException(String.format("Unknown command: %s %s","login",StrArrToSting(argv)));
        }
    }

    public static void testQrLogin(Terminal terminal,PrintStream out) throws IOException {
        terminal.pause();
        out.println("test");
        try {
            HashMap<EncodeHintType, Serializable> hints = new HashMap<EncodeHintType, java.io.Serializable>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");//编码方式
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);//纠错等级
            BitMatrix bitMatrix = new MultiFormatWriter().encode("https://passport.bilibili.com/h5-app/passport/login/scan?navhide=1&qrcode_key=cc2d9b94a2e5cd84c37efdd38aa3f7ab&from=main-fe-header", BarcodeFormat.QR_CODE, 1, 1, hints);
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        terminal.resume();
        biliInstance in = new biliInstance();
        /*in.new Login().new QR().loginWithQrLogin(new Callback() {
            @Override
            public void onSuccess(loginData data, String message, int code) {
                System.out.printf("%d-%s%n",code,message);
                dataManager.getData().getBilibili().setCookies(data.getCookies());
                dataManager.getData().getBilibili().setRefreshToken(data.getRefreshToken());
                dataManager.getData().getBilibili().setLatestRefreshTimestamp(data.getTimestamp());
                terminal.resume();
            }

            @Override
            public void onFailure(Exception e, String cause, int code) {
                System.out.printf("%d-%s%n",code,cause);
                terminal.resume();
            }

            @Override
            public void onUpdate(String message, int code) {
                System.out.printf("%d-%s%n",code,message);
            }

            @Override
            public void onGetQRUrl(String QRUrl) {
                System.out.println(QRUrl);
                try {
                    HashMap<EncodeHintType, Serializable> hints = new HashMap<EncodeHintType, java.io.Serializable>();
                    hints.put(EncodeHintType.CHARACTER_SET, "utf-8");//编码方式
                    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);//纠错等级
                    BitMatrix bitMatrix = new MultiFormatWriter().encode(QRUrl, BarcodeFormat.QR_CODE, 1, 1, hints);
                    for (int j = 0; j < bitMatrix.getHeight(); j++) {
                        for (int i = 0; i < bitMatrix.getWidth(); i++) {
                            if (bitMatrix.get(i, j)) {
                                out.print("\33[48;5;47m█");
                            } else {
                                out.print("\33[0m  ");
                            }

                        }
                        out.println();
                    }
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                flag[0] = false;
            }
        });*/
    }
}
