package me.heartalborada.biliDownloader.Cli.Terminal;

import lombok.SneakyThrows;
import org.jline.keymap.BindingReader;
import org.jline.reader.EndOfFileException;
import org.jline.utils.ClosedException;
import org.jline.utils.NonBlockingReader;

import java.io.IOError;
import java.io.IOException;
import java.io.InterruptedIOException;

public class BindingReaderM extends BindingReader {
    public BindingReaderM(NonBlockingReader reader) {
        super(reader);
    }

    @Override
    @SneakyThrows
    public int readCharacter() {
        if (!pushBackChar.isEmpty()) {
            return pushBackChar.pop();
        }
        try {
            int c = NonBlockingReader.READ_EXPIRED;
            int s = 0;
            while (c == NonBlockingReader.READ_EXPIRED) {
                c = reader.read();
                if (c >= 0 && Character.isHighSurrogate((char) c)) {
                    s = c;
                    c = NonBlockingReader.READ_EXPIRED;
                }
            }
            return s != 0 ? Character.toCodePoint((char) s, (char) c) : c;
        } catch (ClosedException e) {
            throw new EndOfFileException(e);
        } catch (IOException e) {
            if (e instanceof InterruptedIOException)
                return 0x0003;
            throw new IOError(e);
        }
    }
}
